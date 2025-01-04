package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息数据访问接口
 *
 * @author chat
 * @since 2024-01-01
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
} 