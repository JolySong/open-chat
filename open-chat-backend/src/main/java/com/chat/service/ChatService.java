package com.chat.service;

import com.chat.model.dto.MessageDTO;
import com.chat.model.vo.ChatRoomVO;
import com.chat.model.vo.MessageVO;
import com.chat.model.vo.PollResult;

import java.util.List;
import java.util.Set;

/**
 * 聊天服务接口
 *
 * @author chat
 * @since 2024-01-01
 */
public interface ChatService {
    
    /**
     * 创建聊天室
     *
     * @return 聊天室ID
     */
    String createRoom();
    
    /**
     * 加入聊天室
     *
     * @param roomId 聊天室ID
     * @param username 用户名
     */
    void joinRoom(String roomId, String username);
    
    /**
     * 离开聊天室
     *
     * @param roomId 聊天室ID
     * @param username 用户名
     */
    void leaveRoom(String roomId, String username);
    
    /**
     * 删除聊天室
     *
     * @param roomId 聊天室ID
     */
    void deleteRoom(String roomId);
    
    /**
     * 获取聊天室在线人数
     *
     * @param roomId 聊天室ID
     * @return 在线人数
     */
    int getOnlineCount(String roomId);
    
    /**
     * 发送消息
     *
     * @param messageDTO 消息内容
     */
    void sendMessage(MessageDTO messageDTO);
    
    /**
     * 获取聊天室信息
     *
     * @param roomId 聊天室ID
     * @return 聊天室信息
     */
    ChatRoomVO getRoomInfo(String roomId);
    
    /**
     * 获取在线用户列表
     *
     * @param roomId 聊天室ID
     * @return 在线用户集合
     */
    Set<String> getOnlineUsers(String roomId);
    
    /**
     * 获取消息列表
     *
     * @param roomId 聊天室ID
     * @param lastMessageId 最后一条消息ID
     * @return 消息列表
     */
    List<MessageVO> getMessages(String roomId, Long lastMessageId);
    
    /**
     * 长轮询获取更新
     *
     * @param roomId 聊天室ID
     * @param lastMessageId 最后一条消息ID
     * @param timeout 超时时间（秒）
     * @return 轮询结果
     */
    PollResult poll(String roomId, Long lastMessageId, Integer timeout);
} 