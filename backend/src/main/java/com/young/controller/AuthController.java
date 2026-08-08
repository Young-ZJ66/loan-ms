package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.BusinessException;
import com.young.common.LoginRateLimiter;
import com.young.common.RequireRole;
import com.young.common.Result;
import com.young.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证登录管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService userService;
    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || password == null) {
            return Result.error(400, "账号和密码不能为空");
        }
        userService.register(username, password);
        return Result.success("注册成功！");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || password == null) {
            return Result.error(400, "账号和密码不能为空");
        }
        if (loginRateLimiter.isBlocked(username)) {
            return Result.error(429, "登录失败次数过多，请15分钟后再试");
        }
        try {
            String token = userService.login(username, password);
            loginRateLimiter.reset(username);
            return Result.success(token);
        } catch (BusinessException e) {
            loginRateLimiter.recordFailure(username);
            throw e;
        }
    }

    @Operation(summary = "修改当前用户密码")
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录或Token无效");
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.error(400, "密码不能为空");
        }
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "管理员重置用户密码")
    @RequireRole
    @PostMapping("/admin/reset-password/{userId}")
    public Result<?> resetPassword(@PathVariable Long userId, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        if (newPassword == null) return Result.error(400, "新密码不能为空");
        userService.resetPassword(userId, newPassword);
        return Result.success("重置成功");
    }
}
