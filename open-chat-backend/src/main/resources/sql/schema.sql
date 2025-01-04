CREATE DATABASE IF NOT EXISTS open_chat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE open_chat;

-- 聊天室表
CREATE TABLE IF NOT EXISTS chat_room (
    id VARCHAR(32) NOT NULL COMMENT '聊天室ID',
    name VARCHAR(100) COMMENT '聊天室名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天室表';

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id VARCHAR(32) NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    room_id VARCHAR(32) NOT NULL COMMENT '聊天室ID',
    user_id VARCHAR(32) NOT NULL COMMENT '发送者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id),
    KEY idx_room_id (room_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表'; 