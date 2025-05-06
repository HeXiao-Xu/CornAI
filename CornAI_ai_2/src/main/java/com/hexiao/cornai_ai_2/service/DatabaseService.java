package com.hexiao.cornai_ai_2.service;

import com.hexiao.cornai_ai_2.model.SessionInfo;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@Transactional
public class DatabaseService {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 用户注册
    public boolean registerUser(String phoneNumber, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users(phone_number, password_hash) VALUES (?, ?)";
        try {
            int rows = jdbcTemplate.update(sql, phoneNumber, hashedPassword);
            return rows > 0;
        } catch (DataIntegrityViolationException e) {
            return false; // 处理手机号重复的情况
        }
    }

    // 用户登录验证
    public Integer verifyUser(String phoneNumber, String password) {
        String sql = "SELECT id, password_hash FROM users WHERE phone_number = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String storedHash = rs.getString("password_hash");
                return BCrypt.checkpw(password, storedHash) ? rs.getInt("id") : null;
            }, phoneNumber);
        } catch (EmptyResultDataAccessException e) {
            return null; // 用户不存在
        }
    }

    // 新增方法，检查用户是否存在
    public boolean isUserExist(String phoneNumber) {
        String sql = "SELECT COUNT(*) FROM users WHERE phone_number = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, phoneNumber);
        return count > 0;
    }

    // 更新会话名称
    public boolean updateSessionName(int userId, String sessionId, String newName) {
        String sql = "UPDATE sessions SET name = ? WHERE user_id = ? AND session_id = ?";
        return jdbcTemplate.update(sql, newName, userId, sessionId) > 0;
    }

    public boolean deleteSession(int userId, String sessionId) {
        // 1. 删除关联消息
        jdbcTemplate.update("DELETE FROM messages WHERE session_id = ?", sessionId);

        // 2. 删除会话
        String sql = "DELETE FROM sessions WHERE user_id = ? AND session_id = ?";
        return jdbcTemplate.update(sql, userId, sessionId) > 0;
    }

    // 保存消息记录
    public void saveMessage(int userId, String sessionId, String role, String content) {
        String sql = "INSERT INTO messages(user_id, session_id, role, content) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, sessionId, role, content);
    }

    //保存会话
    public boolean saveSession(String sessionId, int userId, String name) {
        String sql = "INSERT INTO sessions(session_id, user_id, name) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, sessionId, userId, name) > 0;
    }

    // DatabaseService.java 修改删除逻辑
    @Transactional
    public void deleteAllSessions(int userId) {
        // 1. 删除关联消息
        jdbcTemplate.update("DELETE FROM messages WHERE session_id IN " +
                "(SELECT session_id FROM sessions WHERE user_id = ?)", userId);

        // 2. 删除用户的所有会话
        jdbcTemplate.update("DELETE FROM sessions WHERE user_id = ?", userId);
    }

    @Transactional
    public boolean deleteUser(int userId) {
        // 1. 级联删除用户的所有会话和消息
        deleteAllSessions(userId);

        // 2. 删除用户
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId) > 0;
    }

    // 加载用户会话
    public List<SessionInfo> loadUserSessions(int userId) {
        String sql = "SELECT session_id, name, create_time, last_updated FROM sessions WHERE user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SessionInfo session = new SessionInfo();
            session.setSessionId(rs.getString("session_id"));
            session.setName(rs.getString("name"));
            session.setCreateTime(rs.getTimestamp("create_time")); // 直接使用Timestamp
            session.setLastUpdated(rs.getTimestamp("last_updated"));
            return session;
        }, userId);
    }

    // 加载消息历史
    public List<String> loadMessageHistory(String sessionId) {
        String sql = "SELECT role, content FROM messages WHERE session_id = ? ORDER BY create_time";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        rs.getString("role") + ": " + rs.getString("content"),
                sessionId
        );
    }

    // DatabaseService.java 新增方法
    public SessionInfo getSession(String sessionId) {
        String sql = "SELECT session_id, name, user_id, create_time, last_updated FROM sessions WHERE session_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            SessionInfo session = new SessionInfo();
            session.setSessionId(rs.getString("session_id"));
            session.setName(rs.getString("name"));
            session.setUserId(rs.getInt("user_id"));
            session.setCreateTime(rs.getTimestamp("create_time"));
            session.setLastUpdated(rs.getTimestamp("last_updated")); // 新增
            return session;
        }, sessionId);
    }



    public String getUserPhoneNumber(int userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT phone_number FROM users WHERE id = ?",
                    String.class,
                    userId
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public String getUserTheme(int userId) {
        return jdbcTemplate.queryForObject(
                "SELECT theme_preference FROM users WHERE id = ?",
                String.class, userId);
    }

    public void updateUserTheme(int userId, String theme) {
        jdbcTemplate.update(
                "UPDATE users SET theme_preference = ? WHERE id = ?",
                theme, userId);
    }
}