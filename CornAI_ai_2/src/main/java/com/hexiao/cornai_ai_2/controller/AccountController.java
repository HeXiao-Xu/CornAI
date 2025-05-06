package com.hexiao.cornai_ai_2.controller;

import com.hexiao.cornai_ai_2.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    @Autowired
    private DatabaseService databaseService;

    @Value("${agreement.user}")
    private String userAgreement;

    @Value("${agreement.privacy}")
    private String privacyPolicy;

    @Value("${agreement.permissions}")
    private String appPermissions;

    @Value("${contact.info}")
    private String contactInfo;

    // 获取账户基本信息
    @GetMapping("/info")
    public ResponseEntity<?> getAccountInfo(@RequestHeader("X-User-ID") int userId) {
        String phoneNumber = databaseService.getUserPhoneNumber(userId);
        String theme = databaseService.getUserTheme(userId);
        return ResponseEntity.ok(new AccountInfo(phoneNumber, theme));
    }

    // 更新主题偏好
//    @PutMapping("/theme")
//    public ResponseEntity<?> updateTheme(
//            @RequestHeader("X-User-ID") int userId,
//            @RequestParam String theme) {
//        databaseService.updateUserTheme(userId, theme);
//        return ResponseEntity.ok().build();
//    }

    // 获取用户协议
    @GetMapping("/agreements/user")
    public ResponseEntity<String> getUserAgreement() {
        return ResponseEntity.ok(userAgreement);
    }

    // 获取隐私政策
    @GetMapping("/agreements/privacy")
    public ResponseEntity<String> getPrivacyPolicy() {
        return ResponseEntity.ok(privacyPolicy);
    }

    // 获取应用权限说明
    @GetMapping("/agreements/permissions")
    public ResponseEntity<String> getAppPermissions() {
        return ResponseEntity.ok(appPermissions);
    }

    // 获取联系方式
    @GetMapping("/contact")
    public ResponseEntity<String> getContactInfo() {
        return ResponseEntity.ok(contactInfo);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(
            @RequestHeader("X-User-ID") int userId) {

        boolean success = databaseService.deleteUser(userId);
        return success ?
                ResponseEntity.noContent().build() :
                ResponseEntity.internalServerError().build();
    }

    // 账户信息DTO
    private static class AccountInfo {
        private final String phoneNumber;
        private final String theme;

        public AccountInfo(String phoneNumber, String theme) {
            this.phoneNumber = phoneNumber;
            this.theme = theme;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getTheme() {
            return theme;
        }
    }
}