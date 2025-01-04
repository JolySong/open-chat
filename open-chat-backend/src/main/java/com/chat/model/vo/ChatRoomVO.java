package com.chat.model.vo;

import lombok.Data;

import java.util.Set;

@Data
public class ChatRoomVO {
    private String id;
    private String name;
    private Set<String> onlineUsers;
    private Integer onlineCount;
} 