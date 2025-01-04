package com.chat.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String roomId;
    private String username;
    private String content;
    private Long lastMessageId;
    private Integer timeout;
}