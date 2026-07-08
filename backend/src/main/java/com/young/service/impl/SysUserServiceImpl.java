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

    // 统一密码混淆盐值，防止彩虹表反查
    private static final String PASSWORD_SALT = "loan_system_salt_2026_@#!";

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

        // 引入盐值进行加盐 MD5 混淆
        String saltedPassword = password + PASSWORD_SALT;
        String md5Password = DigestUtils.md5DigestAsHex(saltedPassword.getBytes());

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

        // 登录验证时使用相同的加盐 MD5 进行比对
        String saltedInput = password + PASSWORD_SALT;
        String inputMd5 = DigestUtils.md5DigestAsHex(saltedInput.getBytes());
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
        
        // 原密码校验加盐 MD5
        String saltedOld = oldPassword + PASSWORD_SALT;
        String oldMd5 = DigestUtils.md5DigestAsHex(saltedOld.getBytes());
        if (!user.getPassword().equals(oldMd5)) {
            throw new RuntimeException("原密码错误");
        }
        
        // 新密码加盐 MD5 存储
        String saltedNew = newPassword + PASSWORD_SALT;
        String newMd5 = DigestUtils.md5DigestAsHex(saltedNew.getBytes());
        userMapper.updatePassword(userId, newMd5);
    }

    @Override
    public void resetPassword(Long targetUserId, String newPassword) {
        // 重置密码加盐 MD5 存储
        String saltedNew = newPassword + PASSWORD_SALT;
        String newMd5 = DigestUtils.md5DigestAsHex(saltedNew.getBytes());
        userMapper.updatePassword(targetUserId, newMd5);
    }
}
