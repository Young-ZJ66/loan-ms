package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || password == null) {
            return Result.error(400, "账号和密码不能为空");
        }
        Long userId = userService.register(username, password);
        return Result.success("注册成功！");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String token = userService.login(username, password);
        return Result.success(token);
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
    @PostMapping("/admin/reset-password/{userId}")
    public Result<?> resetPassword(@PathVariable Long userId, @RequestBody Map<String, String> payload, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) return Result.error(403, "无权限执行此操作");
        String newPassword = payload.get("newPassword");
        if (newPassword == null) return Result.error(400, "新密码不能为空");
        userService.resetPassword(userId, newPassword);
        return Result.success("重置成功");
    }
}
