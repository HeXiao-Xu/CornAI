package com.hexiao.cornai_ai_2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // 登录页
    @GetMapping("/login")
    public String login() {
        return "auth/login"; // 对应 templates/auth/login.html
    }

    // 聊天主界面
    @GetMapping("/chat")
    public String chat() {
        return "chat/index"; // 对应 templates/chat/index.html
    }

    // 注册页
    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    // 新增会话管理页路由
    @GetMapping("/sessions/manage")
    public String sessionManage() {
        return "sessions/manage"; // 对应 templates/sessions/manage.html
    }

    // 新增账户设置页路由
    @GetMapping("/account/settings")
    public String accountSettings() {
        return "account/settings"; // 对应 templates/account/settings.html
    }
}