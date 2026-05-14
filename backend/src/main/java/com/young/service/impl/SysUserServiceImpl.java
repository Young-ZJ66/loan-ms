package com.young.service.impl;

import com.young.mapper.SysUserMapper;
import com.young.mapper.UserCreditMapper;
import com.young.pojo.SysUser;
import com.young.pojo.UserCredit;
import com.young.service.SysUserService;
import com.young.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private UserCreditMapper creditMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String password) {
        SysUser exist = userMapper.selectByUsername(username);
        if (exist != null) {
            throw new RuntimeException("该账号名称已被使用！");
        }

        // 加盐/直接MD5混淆密码
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPassword(md5Password);
        newUser.setRole(0); // 默认注册为客户 0

        userMapper.insert(newUser);

        return newUser.getId();
    }

    @Override
    public String login(String username, String password) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("账号不存在，请检查后重试");
        }

        String inputMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!user.getPassword().equals(inputMd5)) {
            throw new RuntimeException("用户名或密码错误，请重新输入");
        }

        // 生成包含用户身份信息的 JWT Token
        return JwtUtils.generateToken(user.getId(), user.getRole());
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String oldMd5 = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!user.getPassword().equals(oldMd5)) {
            throw new RuntimeException("原密码错误");
        }
        String newMd5 = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        userMapper.updatePassword(userId, newMd5);
    }

    @Override
    public void resetPassword(Long targetUserId, String newPassword) {
        String newMd5 = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        userMapper.updatePassword(targetUserId, newMd5);
    }
}
