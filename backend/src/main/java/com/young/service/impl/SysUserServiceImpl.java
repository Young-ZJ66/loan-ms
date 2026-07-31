package com.young.service.impl;

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
        SysUser exist = userMapper.selectByUsername(username);
        if (exist != null) {
            throw new RuntimeException("该账号名称已被使用！");
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
            throw new RuntimeException("账号不存在，请检查后重试");
        }

        if (!PASSWORD_ENCODER.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误，请重新输入");
        }

        return jwtUtils.generateToken(user.getId(), user.getRole());
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!PASSWORD_ENCODER.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        String encodedNew = PASSWORD_ENCODER.encode(newPassword);
        userMapper.updatePassword(userId, encodedNew);
    }

    @Override
    public void resetPassword(Long targetUserId, String newPassword) {
        String encodedNew = PASSWORD_ENCODER.encode(newPassword);
        userMapper.updatePassword(targetUserId, encodedNew);
    }
}
