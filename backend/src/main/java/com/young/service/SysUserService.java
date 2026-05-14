package com.young.service;

public interface SysUserService {
    /**
     * 注册方法，返回初始化的 userId
     */
    Long register(String username, String password);

    /**
     * 登录验证，通过后返回 JWT token
     */
    String login(String username, String password);

    void changePassword(Long userId, String oldPassword, String newPassword);
    
    void resetPassword(Long targetUserId, String newPassword);
}
