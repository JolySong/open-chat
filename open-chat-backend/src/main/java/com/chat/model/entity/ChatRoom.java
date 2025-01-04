package com.chat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_room")
public class ChatRoom extends BaseEntity {
    
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    
    private String name;
} 