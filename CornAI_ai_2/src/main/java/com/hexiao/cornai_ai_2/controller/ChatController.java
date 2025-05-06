package com.hexiao.cornai_ai_2.controller;

import com.hexiao.cornai_ai_2.model.SessionInfo;
import com.hexiao.cornai_ai_2.dto.MessageRequest;
import com.hexiao.cornai_ai_2.service.DatabaseService;
import com.hexiao.cornai_ai_2.service.RagflowService;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private RagflowService ragflowService;

    @Autowired
    private DatabaseService databaseService;

    // 发送消息
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(
            @RequestBody MessageRequest request,
            @RequestHeader("X-User-ID") int userId
    ) {
        try {
            logger.info("收到消息请求 - 用户:{} 会话:{} 内容:{}", userId, request.getSessionId(), request.getMessage());

            // 1. 保存用户消息
            databaseService.saveMessage(userId, request.getSessionId(), "user", request.getMessage());

            // 2. 调用RAG服务
            String answer = ragflowService.askQuestion(request.getMessage(), request.getSessionId(), request.isStream());
            logger.info("RAG响应: {}", answer);

            // 3. 保存AI回复
            databaseService.saveMessage(userId, request.getSessionId(), "assistant", answer);

            return ResponseEntity.ok().body(Collections.singletonMap("answer", answer));
        } catch (Exception e) {
            logger.error("消息处理失败 - 用户:{} 会话:{}", userId, request.getSessionId(), e);
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", "服务器错误: " + e.getMessage()));
        }
    }

    // 获取会话历史
    @GetMapping("/{sessionId}/history")
    public ResponseEntity<List<String>> getChatHistory(
            @PathVariable String sessionId,
            @RequestHeader("X-User-ID") int userId
    ) {
        try {
            // 1. 验证会话归属
            SessionInfo session = databaseService.getSession(sessionId);
            if (session == null || session.getUserId() != userId) {
                return ResponseEntity.notFound().build();
            }

            // 2. 查询历史记录
            List<String> history = databaseService.loadMessageHistory(sessionId);
            if (history == null || history.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList()); // 返回空数组而非null
            }

            return ResponseEntity.ok(history);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("加载历史消息失败 - 会话:{} 用户:{}", sessionId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonList("加载失败: " + e.getMessage()));
        }
    }

    // 重命名会话
    @PutMapping("/{sessionId}")
    public ResponseEntity<?> renameSession(
            @PathVariable String sessionId,
            @RequestParam String newName,
            @RequestHeader("X-User-ID") int userId) {

        boolean success = databaseService.updateSessionName(userId, sessionId, newName);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    // 删除会话
    // ChatController.java 修改删除端点
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-ID") int userId
    ) {
        try {
            // 1. 先删除远程会话
            ragflowService.deleteSession(sessionId);

            // 2. 再删除本地记录
            boolean dbSuccess = databaseService.deleteSession(userId, sessionId);

            return dbSuccess ?
                    ResponseEntity.ok().build() :
                    ResponseEntity.internalServerError().body("本地删除失败");

        } catch (IOException | JSONException e) {
            return ResponseEntity.status(502)
                    .body("远程服务错误: " + e.getMessage());
        }
    }

    // 批量获取会话列表
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> listSessions(
            @RequestHeader("X-User-ID") int userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<SessionInfo> sessions = databaseService.loadUserSessions(userId);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(sessions.size()))
                .body(sessions);
    }

    // ChatController.java 新增获取会话详情
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionInfo> getSession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-ID") int userId
    ) {
        try {
            SessionInfo session = databaseService.getSession(sessionId);
            if (session.getUserId() != userId) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(session);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }
}