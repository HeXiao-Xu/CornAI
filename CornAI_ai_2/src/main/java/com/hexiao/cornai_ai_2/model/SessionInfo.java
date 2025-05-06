package com.hexiao.cornai_ai_2.model;

import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
public class SessionInfo {
    private String sessionId;
    private String name;
    private int userId;
    private Timestamp createTime;
    private boolean nameUpdated;
    private Timestamp lastUpdated;
    private List<String> history = new ArrayList<>();

    public SessionInfo(String sessionId, String name, int userId, Timestamp createTime) {
        this.sessionId = sessionId;
        this.name = name;
        this.userId = userId; // 确保不为null
        this.createTime = createTime;
        this.nameUpdated = false;
    }

    public SessionInfo() {

    }
}