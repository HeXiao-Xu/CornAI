package com.hexiao.cornai_ai_2.dto;

import lombok.Data;

@Data
public class MessageRequest {
    private String sessionId;
    private String message;
    private boolean stream;
}