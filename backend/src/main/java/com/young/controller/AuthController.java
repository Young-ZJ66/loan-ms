package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.BusinessException;
import com.young.common.LoginRateLimiter;
import com.young.common.RequireRole;
import com.young.common.Result;
import com.young.service.SysUserService;
import com.young.utils.JwtUtils;
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
    @Autowired
    private JwtUtils jwtUtils;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String ip = getClientIp(request);
        if (loginRateLimiter.isIpBlocked(ip)) {
            return Result.error(429, "该IP注册请求过于频繁，请15分钟后再试");
        }
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || password == null) {
            return Result.error(400, "账号和密码不能为空");
        }
        try {
            userService.register(username, password);
            return Result.success("注册成功！");
        } catch (BusinessException e) {
            loginRateLimiter.recordFailure(null, ip);
            throw e;
        }
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || password == null) {
            return Result.error(400, "账号和密码不能为空");
        }
        String ip = getClientIp(request);
        if (loginRateLimiter.isBlocked(username) || loginRateLimiter.isIpBlocked(ip)) {
            return Result.error(429, "登录失败次数过多，请15分钟后再试");
        }
        try {
            String token = userService.login(username, password);
            loginRateLimiter.reset(username);
            return Result.success(token);
        } catch (BusinessException e) {
            loginRateLimiter.recordFailure(username, ip);
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
        // 改密后立即吊销当前 token，强制重新登录
        invalidateCurrentToken(request);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        invalidateCurrentToken(request);
        return Result.success("已退出登录");
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

    /**
     * 吊销当前请求的 token（加入黑名单）
     */
    private void invalidateCurrentToken(HttpServletRequest request) {
        String jti = (String) request.getAttribute("jti");
        Long exp = (Long) request.getAttribute("exp");
        if (jti != null) {
            // 若无 exp 属性则用默认 2 小时后过期
            long expMillis = exp != null ? exp : System.currentTimeMillis() + 2 * 60 * 60 * 1000L;
            jwtUtils.invalidate(jti, expMillis);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
