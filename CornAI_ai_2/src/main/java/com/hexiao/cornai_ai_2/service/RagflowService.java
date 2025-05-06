package com.hexiao.cornai_ai_2.service;

import com.hexiao.cornai_ai_2.model.SessionInfo;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class RagflowService {
    @Value("${ragflow.api.url}")
    private String baseUrl;

    @Value("${ragflow.api.key}")
    private String apiKey;

    @Value("${ragflow.chat.id}")
    private String chatId;

    private final OkHttpClient client;

    @Autowired
    public RagflowService(OkHttpClient client) {
        this.client = client;
    }

    // 创建会话
    public String createSession(String name, String userId) throws IOException, JSONException {
//        if (name == null || name.trim().isEmpty()) {
//            throw new IllegalArgumentException("会话名称不能为空");
//        }
        JSONObject body = new JSONObject();
        body.put("name", name.trim());
        if (userId != null && !userId.trim().isEmpty()) {
            body.put("user_id", userId.trim());
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/chats/" + chatId + "/sessions")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        // 关键修改：使用try-with-resources确保Response正确关闭
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP请求失败: " + response.code());
            }

            // 关键修改：缓存响应体内容
            String responseBody = response.body().string(); // 只能调用一次
            JSONObject json = new JSONObject(responseBody);

            // 验证响应结构
            if (!json.has("data") || !json.getJSONObject("data").has("id")) {
                throw new JSONException("无效的响应格式");
            }

            return json.getJSONObject("data").getString("id");
        }
    }

    // 更新会话名称
    public void updateSession(String sessionId, String newName) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("name", newName);
        Request request = new Request.Builder()
                .url(baseUrl + "/chats/" + chatId + "/sessions/" + sessionId)
                .put(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            validateResponse(response);
        }
    }

    // 删除所有会话
    public void deleteSession(String sessionId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(baseUrl + "/chats/" + chatId + "/sessions/")
                .delete()
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            validateResponse(response);
        }
    }

    // 获取会话列表（分页）
    public List<SessionInfo> listSessions(int page, int pageSize, int size) throws IOException, JSONException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/chats/" + chatId + "/sessions")
                .newBuilder()
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("size", String.valueOf(pageSize))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            validateResponse(response);
            JSONObject json = new JSONObject(response.body().string());
            return parseSessionList(json.getJSONArray("data"));
        }
    }

    // 发送问题（支持流式响应）
    public String askQuestion(String question, String sessionId, boolean stream)
            throws IOException, JSONException {

        JSONObject body = new JSONObject();
        body.put("question", question);
        body.put("stream", stream);
        body.put("session_id", sessionId);

        Request request = new Request.Builder()
                .url(baseUrl + "/chats/" + chatId + "/completions")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP错误: " + response.code() + " - " + response.body().string());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);

            // 更健壮的响应解析
            if (json.has("code") && json.getInt("code") != 0) {
                throw new IOException("RAG服务错误: " + json.optString("message"));
            }
            if (!json.has("data") || !json.getJSONObject("data").has("answer")) {
                throw new JSONException("响应缺少必要字段");
            }

            return json.getJSONObject("data").getString("answer");
        }
    }

    private void validateResponse(Response response) throws IOException, JSONException {
        if (!response.isSuccessful()) {
            throw new IOException("HTTP错误: " + response.code());
        }

        // 缓存响应体内容
        String responseBody = response.body().string();
        JSONObject json = new JSONObject(responseBody);
        if (json.getInt("code") != 0) {
            throw new IOException("API错误: " + json.optString("message"));
        }
    }


    private String processStreamResponse(Response response) throws IOException, JSONException {
        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    JSONObject data = new JSONObject(line.substring(5).trim());
                    if (data.has("answer")) {
                        answer.append(data.getString("answer"));
                    }
                }
            }
        }
        return answer.toString();
    }


    // 私有方法：解析会话列表
    private List<SessionInfo> parseSessionList(JSONArray data) throws JSONException {
        List<SessionInfo> sessions = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            SessionInfo session = new SessionInfo();
            session.setSessionId(item.getString("session_id"));
            session.setName(item.getString("name"));
            session.setCreateTime(Timestamp.valueOf(item.getString("create_time")));
            sessions.add(session);
        }
        return sessions;
    }

    public SessionInfo getSessionDetails(String sessionId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(baseUrl + "/chats/" + chatId + "/sessions/" + sessionId)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            JSONObject json = new JSONObject(response.body().string());
            return parseSessionDetails(json.getJSONObject("data"));
        }
    }

    private SessionInfo parseSessionDetails(JSONObject data) throws JSONException {
        SessionInfo session = new SessionInfo();
        session.setSessionId(data.getString("id"));
        session.setName(data.getString("name"));
        session.setCreateTime(Timestamp.valueOf(data.getString("create_time")));
        return session;
    }
}