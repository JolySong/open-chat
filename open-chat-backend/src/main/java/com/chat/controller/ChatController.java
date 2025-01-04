package com.chat.controller;

import com.chat.model.dto.MessageDTO;
import com.chat.model.vo.ApiResponse;
import com.chat.model.vo.ChatRoomVO;
import com.chat.model.vo.PollResult;
import com.chat.service.ChatService;
import com.chat.util.LongPollingUtil;
import com.chat.annotation.Crypto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 聊天室控制器
 *
 * @author chat
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/room")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Value(value = "${chat.timeout:10000}")
    private Integer timeout;

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 创建聊天室
     *
     * @return 聊天室ID
     */
    @PostMapping("/create")
    @Crypto(decryptRequest = false)
    public ApiResponse<Map<String, String>> createRoom() {
        String roomId = chatService.createRoom();
        Map<String, String> data = new HashMap<>();
        data.put("roomId", roomId);
        return ApiResponse.success(data);
    }

    /**
     * 加入聊天室
     */
    @PostMapping("/join")
    @Crypto
    public ApiResponse<PollResult> joinRoom(@RequestBody MessageDTO messageDTO) {
        log.info("用户加入聊天室: roomId={}, username={}", 
            messageDTO.getRoomId(), messageDTO.getUsername());
        
        chatService.joinRoom(messageDTO.getRoomId(), messageDTO.getUsername());
        
        // 通知其他用户更新在线列表
        PollResult pollResult = new PollResult();
        Set<String> users = chatService.getOnlineUsers(messageDTO.getRoomId());
        log.info("当前在线用户: roomId={}, users={}", messageDTO.getRoomId(), users);
        pollResult.setOnlineUsers(users);
        pollResult.setMessages(Collections.emptyList());
        pollResult.setLastMessageId(null);
        
        LongPollingUtil.notifyUserListUpdate(messageDTO.getRoomId(), pollResult);
        
        // 返回当前在线用户列表
        PollResult initialResult = new PollResult();
        initialResult.setOnlineUsers(users);
        initialResult.setMessages(Collections.emptyList());
        initialResult.setLastMessageId(null);
        
        return ApiResponse.success(initialResult);
    }

    /**
     * 离开聊天室
     */
    @PostMapping("/leave")
    @Crypto
    public ApiResponse<Void> leaveRoom(@RequestBody MessageDTO messageDTO) {
        log.info("用户离开聊天室: roomId={}, username={}", 
            messageDTO.getRoomId(), messageDTO.getUsername());
        
        // 先获取当前在线用户
        Set<String> beforeUsers = chatService.getOnlineUsers(messageDTO.getRoomId());
        log.info("离开前在线用户: {}", beforeUsers);
        
        chatService.leaveRoom(messageDTO.getRoomId(), messageDTO.getUsername());
        
        // 检查聊天室是否还存在
        ChatRoomVO roomInfo = chatService.getRoomInfo(messageDTO.getRoomId());
        if (roomInfo == null) {
            log.info("聊天室已删除: {}", messageDTO.getRoomId());
            // 通知其他用户聊天室已删除
            PollResult deleteResult = new PollResult();
            deleteResult.setOnlineUsers(Collections.emptySet());
            deleteResult.setMessages(Collections.emptyList());
            LongPollingUtil.notifyUserListUpdate(messageDTO.getRoomId(), deleteResult);
            return ApiResponse.success(null);
        }
        
        // 通知其他用户更新在线列表
        PollResult pollResult = new PollResult();
        Set<String> afterUsers = chatService.getOnlineUsers(messageDTO.getRoomId());
        log.info("离开后在线用户: {}", afterUsers);
        pollResult.setOnlineUsers(afterUsers);
        pollResult.setMessages(Collections.emptyList());
        pollResult.setLastMessageId(null);
        
        // 只有在用户列表确实发生变化时才通知
        if (!beforeUsers.equals(afterUsers)) {
            LongPollingUtil.notifyUserListUpdate(messageDTO.getRoomId(), pollResult);
        }
        
        return ApiResponse.success(null);
    }

    /**
     * 发送消息
     *
     * @param messageDTO 消息内容
     * @return 操作结果
     */
    @PostMapping("/message")
    @Crypto
    public ApiResponse<Void> sendMessage(@RequestBody @Validated MessageDTO messageDTO) {
        chatService.sendMessage(messageDTO);
        return ApiResponse.success(null);
    }

    /**
     * 获取聊天室信息
     */
    @PostMapping("/info")
    @Crypto
    public ApiResponse<ChatRoomVO> getRoomInfo(@RequestBody MessageDTO messageDTO) {
        return ApiResponse.success(chatService.getRoomInfo(messageDTO.getRoomId()));
    }

    /**
     * 获取在线用户列表
     */
    @PostMapping("/users")
    @Crypto
    public DeferredResult<ApiResponse<PollResult>> getOnlineUsers(@RequestBody MessageDTO messageDTO) {
        String roomId = messageDTO.getRoomId();
        Integer timeout = messageDTO.getTimeout();
        log.info("获取在线用户请求: roomId={}, timeout={}", roomId, timeout);

        // 先检查当前在线用户
        PollResult result = new PollResult();
        Set<String> users = chatService.getOnlineUsers(roomId);
        log.info("当前在线用户: roomId={}, users={}", roomId, users);
        result.setOnlineUsers(users);
        result.setMessages(Collections.emptyList());

        // 创建长轮询请求
        DeferredResult<ApiResponse<PollResult>> deferredResult = new DeferredResult<>(
            timeout != null ? timeout * 1000L : 30000L,
            ApiResponse.success(result)
        );

        // 添加到长轮询管理器
        LongPollingUtil.addUserListRequest(roomId, deferredResult);

        // 设置超时处理
        deferredResult.onTimeout(() -> {
            log.info("用户列表轮询超时: {}", roomId);
            deferredResult.setResult(ApiResponse.success(result));
        });

        return deferredResult;
    }

    /**
     * 获取消息列表（长轮询）
     */
    @PostMapping("/messages")
    @Crypto
    public DeferredResult<ApiResponse<PollResult>> getMessages(@RequestBody MessageDTO messageDTO) {
        String roomId = messageDTO.getRoomId();
        Long lastMessageId = messageDTO.getLastMessageId();

        log.info("获取消息请求: roomId={}, lastMessageId={}, timeout={}", roomId, lastMessageId, timeout);

        // 先检查是否有新消息
        PollResult result = chatService.poll(roomId, lastMessageId, timeout);
        log.info("初始查询结果: {}", result);

        if (result != null && result.getMessages() != null && !result.getMessages().isEmpty()) {
            return new DeferredResult<ApiResponse<PollResult>>() {{
                setResult(ApiResponse.success(result));
            }};
        }

        // 没有新消息，创建长轮询请求
        DeferredResult<Object> deferredResult = LongPollingUtil.createRequest(roomId, timeout);
        
        // 创建新的DeferredResult，使用相同的超时时间
        DeferredResult<ApiResponse<PollResult>> typedResult = new DeferredResult<>(
                (long) timeout,
            ApiResponse.success(new PollResult())
        );

        // 设置结果处理器
        deferredResult.onCompletion(() -> {
            Object pollResult = deferredResult.getResult();
            log.info("长轮询完成，结果类型: {}, 内容: {}", 
                pollResult != null ? pollResult.getClass().getName() : "null", 
                pollResult);

            if (pollResult instanceof LongPollingUtil.TimeoutResult) {
                typedResult.setResult(ApiResponse.success(chatService.poll(roomId, lastMessageId, 0)));
            } else if (pollResult instanceof PollResult) {
                typedResult.setResult(ApiResponse.success((PollResult) pollResult));
            }
        });

        return typedResult;
    }
} 