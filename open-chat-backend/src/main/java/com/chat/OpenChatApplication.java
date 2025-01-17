package com.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.chat.mapper")
public class OpenChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenChatApplication.class, args);
    }
} 