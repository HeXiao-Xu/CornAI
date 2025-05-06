package com.hexiao.cornai_ai_2.controller;

import com.hexiao.cornai_ai_2.model.SessionInfo;
import com.hexiao.cornai_ai_2.service.DatabaseService;
import com.hexiao.cornai_ai_2.service.RagflowService;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private RagflowService ragflowService;
    @Autowired
    private DatabaseService databaseService;

    // 创建会话
    @PostMapping
    public ResponseEntity<?> createSession(
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-ID") int userId
    ) {
        try {
            String name = request.getOrDefault("name", "新建会话");
            logger.info("创建会话请求 - 用户:{} 名称:{}", userId, name);

            // 添加超时控制
            String sessionId = ragflowService.createSession(name, String.valueOf(userId));

            // 数据库操作
            boolean saved = databaseService.saveSession(sessionId, userId, name);
            if (!saved) {
                return ResponseEntity.internalServerError().body("数据库保存失败");
            }

            return ResponseEntity.ok(Collections.singletonMap("sessionId", sessionId));
        } catch (JSONException e) {
            logger.error("RAG响应解析失败", e);
            return ResponseEntity.badRequest().body("AI服务响应格式错误");
        } catch (IOException e) {
            logger.error("RAG服务通信失败", e);
            return ResponseEntity.status(502).body("AI服务不可用: " + e.getMessage());
        } catch (Exception e) {
            logger.error("服务器内部错误", e);
            return ResponseEntity.internalServerError().body("服务器错误: " + e.getMessage());
        }
    }

    // 获取会话列表（分页）
    @GetMapping
    public ResponseEntity<List<SessionInfo>> listSessions(
            @RequestHeader("X-User-ID") int userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            List<SessionInfo> sessions = ragflowService.listSessions(userId, page, size);
            return ResponseEntity.ok(sessions);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(List.of());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // 更新会话名称
    @PutMapping("/{sessionId}")
    public ResponseEntity<?> updateSession(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request, // 修改为接收JSON
            @RequestHeader("X-User-ID") int userId
    ) {
        try {
            String newName = request.get("newName");
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("新名称无效");
            }

            ragflowService.updateSession(sessionId, newName);
            boolean dbSuccess = databaseService.updateSessionName(userId, sessionId, newName);

            return dbSuccess ?
                    ResponseEntity.ok().build() :
                    ResponseEntity.internalServerError().body("数据库更新失败");

        } catch (IOException e) {
            return ResponseEntity.status(502).body("远程服务不可用");
        } catch (JSONException e) {
            return ResponseEntity.badRequest().body("响应格式错误");
        }
    }

    // 删除会话
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-ID") int userId
    ) {

            // 2. 删除本地数据库记录
            boolean dbSuccess = databaseService.deleteSession(userId, sessionId);

            if (!dbSuccess) {
                return ResponseEntity.internalServerError().body("本地数据库删除失败");
            }

            return ResponseEntity.noContent().build();


    }

    // 获取会话详情
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionInfo> getSessionDetails(
            @PathVariable String sessionId,
            @RequestHeader("X-User-ID") int userId
    ) {
        try {
            SessionInfo session = ragflowService.getSessionDetails(sessionId);
            return ResponseEntity.ok(session);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // SessionController.java 新增接口
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAllSessions(
            @RequestHeader("X-User-ID") int userId) {

        try {
            databaseService.deleteAllSessions(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("清空会话失败: " + e.getMessage());
        }
    }
}