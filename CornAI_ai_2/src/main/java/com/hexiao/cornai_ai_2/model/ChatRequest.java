package com.hexiao.cornai_ai_2.model;

public class ChatRequest {
    private String message;
    private boolean stream;

    // Getters & Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
}