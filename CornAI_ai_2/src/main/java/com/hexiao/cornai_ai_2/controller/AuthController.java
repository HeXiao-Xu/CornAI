package com.hexiao.cornai_ai_2.controller;

import com.hexiao.cornai_ai_2.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private DatabaseService databaseService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String phoneNumber, @RequestParam String password) {
        if (phoneNumber.length() != 11) {
            return ResponseEntity.badRequest().body("手机号必须为11位");
        }
        boolean success = databaseService.registerUser(phoneNumber, password);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body("注册失败");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String phoneNumber, @RequestParam String password) {
        Integer userId = databaseService.verifyUser(phoneNumber, password);
        if (userId != null) {
            return ResponseEntity.ok().body(String.valueOf(userId));
        } else {
            if (databaseService.isUserExist(phoneNumber)) {
                return ResponseEntity.status(401).body("密码错误");
            } else {
                return ResponseEntity.status(401).body("用户不存在");
            }
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteUserAccount(
            @RequestHeader("X-User-ID") int userId,
            @RequestParam String password,
            @RequestParam String confirmCode) {

        if (!"CONFIRM_DELETE".equals(confirmCode)) {
            return ResponseEntity.badRequest().body("需要确认操作");
        }

        // 1. 获取用户手机号
        String phoneNumber = databaseService.getUserPhoneNumber(userId);
        if (phoneNumber == null) {
            return ResponseEntity.status(404).body("用户不存在");
        }

        // 2. 验证密码
        Integer verifiedUserId = databaseService.verifyUser(phoneNumber, password);
        if (verifiedUserId == null || verifiedUserId != userId) {
            return ResponseEntity.status(401).body("密码错误");
        }

        // 3. 删除用户及其所有数据
        boolean success = databaseService.deleteUser(userId);
        return success ?
                ResponseEntity.ok().build() :
                ResponseEntity.internalServerError().body("账户删除失败");
    }
}