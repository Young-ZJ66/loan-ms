package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.SysUserMapper;
import com.young.pojo.SysUser;
import com.young.service.SysUserService;
import com.young.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysUserServiceImpl implements SysUserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String password) {
        validateUsername(username);
        validatePasswordStrength(password);

        SysUser exist = userMapper.selectByUsername(username);
        if (exist != null) {
            throw new BusinessException("该账号名称已被使用！");
        }

        String encodedPassword = PASSWORD_ENCODER.encode(password);

        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPassword(encodedPassword);
        newUser.setRole(0);

        userMapper.insert(newUser);

        return newUser.getId();
    }

    @Override
    public String login(String username, String password) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误，请重新输入");
        }

        if (!PASSWORD_ENCODER.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误，请重新输入");
        }

        return jwtUtils.generateToken(user.getId(), user.getRole());
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!PASSWORD_ENCODER.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        validatePasswordStrength(newPassword);

        String encodedNew = PASSWORD_ENCODER.encode(newPassword);
        userMapper.updatePassword(userId, encodedNew);
    }

    @Override
    public void resetPassword(Long targetUserId, String newPassword) {
        validatePasswordStrength(newPassword);
        String encodedNew = PASSWORD_ENCODER.encode(newPassword);
        userMapper.updatePassword(targetUserId, encodedNew);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("账号不能为空");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new BusinessException("账号长度需在3-50位之间");
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码长度不能少于8位");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException("密码必须同时包含字母和数字");
        }
    }
}
