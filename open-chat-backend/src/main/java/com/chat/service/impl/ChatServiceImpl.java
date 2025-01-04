package com.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chat.mapper.ChatMessageMapper;
import com.chat.mapper.ChatRoomMapper;
import com.chat.model.dto.MessageDTO;
import com.chat.model.entity.ChatMessage;
import com.chat.model.entity.ChatRoom;
import com.chat.model.vo.ChatRoomVO;
import com.chat.model.vo.MessageVO;
import com.chat.model.vo.PollResult;
import com.chat.service.ChatService;
import com.chat.util.LongPollingUtil;
import com.chat.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 聊天服务实现类
 *
 * @author chat
 * @since 2024-01-01
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RedisUtil redisUtil;


    public ChatServiceImpl(
            ChatRoomMapper chatRoomMapper,
            ChatMessageMapper chatMessageMapper,
            RedisUtil redisUtil
    ) {
        this.chatRoomMapper = chatRoomMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.redisUtil = redisUtil;
    }
    
    /** Redis中存储聊天室在线用户的key模板 */
    private static final String ROOM_ONLINE_USERS_KEY = "chat:room:%s:users";
    /** Redis中存储聊天室消息队列的key模板 */
    private static final String ROOM_MESSAGE_QUEUE_KEY = "chat:room:%s:messages";
    /** 最大轮询时间（秒） */
    private static final int MAX_POLL_TIMEOUT = 30;

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public String createRoom() {
        try {
            ChatRoom room = new ChatRoom();
            String roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 11);
            room.setId(roomId);
            room.setName("Room-" + roomId.substring(0, 6));
            
            int rows = chatRoomMapper.insert(room);
            if (rows != 1) {
                throw new RuntimeException("Failed to create chat room");
            }
            
            log.info("Chat room created successfully: {}", roomId);
            return room.getId();
        } catch (Exception e) {
            log.error("Failed to create chat room", e);
            throw new RuntimeException("Failed to create chat room", e);
        }
    }

    @Override
    public void joinRoom(String roomId, String username) {
        String key = String.format(ROOM_ONLINE_USERS_KEY, roomId);
        log.info("用户加入聊天室: roomId={}, username={}", roomId, username);
        redisUtil.sSet(key, username);
        redisUtil.expire(key, 24, TimeUnit.HOURS);
    }

    @Override
    public void leaveRoom(String roomId, String username) {
        String key = String.format(ROOM_ONLINE_USERS_KEY, roomId);
        log.info("用户离开聊天室: roomId={}, username={}", roomId, username);
        
        // 检查用户是否在聊天室中
        Set<String> users = getOnlineUsers(roomId);
        if (!users.contains(username)) {
            log.info("用户不在聊天室中: roomId={}, username={}", roomId, username);
            return;
        }
        
        redisUtil.setRemove(key, username);
        
        // 检查是否是最后一个用户
        int onlineCount = getOnlineCount(roomId);
        log.info("当前在线人数: {}", onlineCount);
        if (onlineCount == 0) {
            log.info("Last user left, deleting chat room: {}", roomId);
            deleteRoom(roomId);
            // 删除Redis中的用户列表
            redisUtil.del(key);
        }
    }

    @Override
    public void sendMessage(MessageDTO messageDTO) {
        log.info("发送消息: {}", messageDTO);
        // 保存消息到数据库
        ChatMessage message = new ChatMessage();
        message.setRoomId(messageDTO.getRoomId());
        message.setUserId(messageDTO.getUsername());
        message.setContent(messageDTO.getContent());
        chatMessageMapper.insert(message);

        log.info("消息已保存到数据库, id: {}", message.getId());
        // 发送消息到Redis队列
        String key = String.format(ROOM_MESSAGE_QUEUE_KEY, messageDTO.getRoomId());
        MessageVO messageVO = new MessageVO();
        BeanUtils.copyProperties(message, messageVO);
        messageVO.setUsername(messageDTO.getUsername());
        messageVO.setId(message.getId());
        messageVO.setCreatedAt(message.getCreatedAt());
        redisUtil.lSet(key, messageVO);

        log.info("消息已发送到Redis: {}", messageVO);
        // 通知所有等待的长轮询请求
        PollResult pollResult = new PollResult();
        // 获取最新的消息列表
        List<MessageVO> latestMessages = getMessages(messageDTO.getRoomId(), 0L);
        pollResult.setMessages(latestMessages);
        pollResult.setLastMessageId(message.getId());
        pollResult.setOnlineUsers(getOnlineUsers(messageDTO.getRoomId()));
        log.info("通知所有用户新消息: {}", pollResult);
        LongPollingUtil.notifyRoom(messageDTO.getRoomId(), pollResult);
    }

    @Override
    public ChatRoomVO getRoomInfo(String roomId) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if (room == null) {
            return null;
        }

        ChatRoomVO vo = new ChatRoomVO();
        BeanUtils.copyProperties(room, vo);
        Set<String> onlineUsers = getOnlineUsers(roomId);
        vo.setOnlineUsers(onlineUsers);
        vo.setOnlineCount(onlineUsers.size());
        return vo;
    }

    @Override
    public Set<String> getOnlineUsers(String roomId) {
        String key = String.format(ROOM_ONLINE_USERS_KEY, roomId);
        Set<Object> members = redisUtil.sGet(key);
        log.info("获取在线用户列表: roomId={}, members={}", roomId, members);
        Set<String> users = new HashSet<>();
        if (members != null) {
            members.forEach(member -> users.add((String) member));
            redisUtil.expire(key, 24, TimeUnit.HOURS);
        }
        // 返回不可变集合，避免外部修改
        return Collections.unmodifiableSet(users);
    }

    @Override
    public List<MessageVO> getMessages(String roomId, Long lastMessageId) {
//        log.info("获取消息列表: roomId={}, lastMessageId={}", roomId, lastMessageId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getRoomId, roomId)
                .gt(lastMessageId != null && lastMessageId > 0, ChatMessage::getId, lastMessageId)
                .eq(ChatMessage::getDeleted, false)
                .orderByDesc(ChatMessage::getId)  // 按ID降序排序
                .last("LIMIT 50");  // 限制返回最新的50条消息
        
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        // 反转列表以保持时间顺序
        Collections.reverse(messages);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<MessageVO> voList = convertToMessageVOList(messages);
        return voList;
    }

    @Override
    public PollResult poll(String roomId, Long lastMessageId, Integer timeout) {
        log.info("开始轮询: roomId={}, lastMessageId={}, timeout={}", roomId, lastMessageId, timeout);
        // 限制超时时间在 1-30 秒之间
        timeout = timeout != null ? Math.min(Math.max(timeout, 1), 30) : 30;
        timeout *= 1000; // 转换为毫秒
        long endTime = System.currentTimeMillis() + timeout;
        
        PollResult result = new PollResult();
        result.setMessages(new ArrayList<>());
        result.setOnlineUsers(getOnlineUsers(roomId));

        while (System.currentTimeMillis() < endTime) {
            // 检查是否有新消息
            List<MessageVO> messages = getMessages(roomId, lastMessageId);
            if (!messages.isEmpty()) {
                result.setMessages(messages);
                result.setLastMessageId(messages.get(messages.size() - 1).getId());
                log.info("返回轮询结果: {}", result);
                return result;
            }
            
            // 没有新消息，等待一段时间后重试
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 超时返回空结果
        result.setLastMessageId(lastMessageId);
        log.info("轮询超时，返回空结果: {}", result);
        return result;
    }

    /**
     * 将消息实体列表转换为VO列表
     *
     * @param messages 消息实体列表
     * @return 消息VO列表
     */
    private List<MessageVO> convertToMessageVOList(List<ChatMessage> messages) {
        List<MessageVO> voList = new ArrayList<>();
        for (ChatMessage message : messages) {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(message, vo);
            vo.setUsername(message.getUserId());
            vo.setId(message.getId());
            vo.setContent(message.getContent());
            vo.setCreatedAt(message.getCreatedAt());
            log.info("转换消息: {} -> {}", message, vo);
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public int getOnlineCount(String roomId) {
        String key = String.format(ROOM_ONLINE_USERS_KEY, roomId);
        Set<Object> members = redisUtil.sGet(key);
        return members != null ? members.size() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoom(String roomId) {
        try {
            log.info("Starting to delete chat room: {}", roomId);
            
            // 1. 删除聊天室记录
            ChatRoom room = new ChatRoom();
            room.setId(roomId);
            room.setDeleted(true);
            chatRoomMapper.updateById(room);
            
            // 2. 删除聊天记录
            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getRoomId, roomId);
            ChatMessage message = new ChatMessage();
            message.setDeleted(true);
            chatMessageMapper.update(message, wrapper);
            
            // 3. 删除Redis中的数据
            String usersKey = String.format(ROOM_ONLINE_USERS_KEY, roomId);
            String messagesKey = String.format(ROOM_MESSAGE_QUEUE_KEY, roomId);
            redisUtil.del(usersKey, messagesKey);
            
            log.info("Chat room deleted successfully: {}", roomId);
        } catch (Exception e) {
            log.error("Failed to delete chat room: {}", roomId, e);
            throw new RuntimeException("Failed to delete chat room", e);
        }
    }
} 