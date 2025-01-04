package com.chat.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class PollResult {
    private List<MessageVO> messages;
    private Set<String> onlineUsers;
    private Long lastMessageId;

    // 添加getter方法确保序列化
    public List<MessageVO> getMessages() {
        return messages;
    }

    public Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    @Override
    public String toString() {
        return "PollResult{" +
                "messages=" + messages +
                ", onlineUsers=" + onlineUsers +
                ", lastMessageId=" + lastMessageId +
                '}';
    }
} 