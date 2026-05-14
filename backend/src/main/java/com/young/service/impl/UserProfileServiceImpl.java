package com.young.service.impl;

import com.young.mapper.UserProfileMapper;
import com.young.pojo.UserProfile;
import com.young.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserProfileMapper profileMapper;

    @Autowired
    private com.young.mapper.SysMessageMapper messageMapper;

    @Override
    public void submitKyc(UserProfile data) {
        UserProfile exist = profileMapper.selectByUserId(data.getUserId());
        if (exist != null) {
            if (exist.getStatus() != null && exist.getStatus() == 2) {
                // Allows update if previously rejected
                data.setId(exist.getId());
                data.setStatus(0);
                profileMapper.update(data);
                return;
            } else {
                throw new RuntimeException("您已经提交过实名材料，请勿重复发起");
            }
        }
        
        data.setStatus(0); // 待管理员审核
        profileMapper.insert(data);
    }

    @Override
    public UserProfile getMyProfile(Long userId) {
        return profileMapper.selectByUserId(userId);
    }

    @Override
    public List<UserProfile> getPendingKycList() {
        return profileMapper.selectPendingList();
    }

    @Override
    public List<UserProfile> getAllProfileList() {
        return profileMapper.selectAllList();
    }

    @Override
    public void auditKyc(Long adminId, Long profileId, boolean isPass, String reason) {
        UserProfile p = profileMapper.selectById(profileId);
        if (p == null || p.getStatus() != 0) return;
        
        // 1-通过，2-驳回
        p.setStatus(isPass ? 1 : 2);
        p.setAuditTime(new Date());
        profileMapper.update(p);

        com.young.pojo.SysMessage msg = new com.young.pojo.SysMessage();
        msg.setToUserId(p.getUserId());
        msg.setIsRead(0);
        
        if (!isPass && reason != null && !reason.trim().isEmpty()) {
            msg.setTitle("实名认证被驳回");
            msg.setContent("您的实名认证被驳回，原因：" + reason + "。请修改后重新提交。");
            messageMapper.insert(msg);
        } else if (isPass) {
            msg.setTitle("实名认证已通过");
            msg.setContent("您的实名认证已通过，现在可以前往申请贷款了。");
            messageMapper.insert(msg);
        }
    }
}
