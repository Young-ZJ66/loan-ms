package com.young.service.impl;

import com.young.common.BusinessException;
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

    @Override
    public void submitKyc(UserProfile data) {
        UserProfile exist = profileMapper.selectByUserId(data.getUserId());
        if (exist != null && exist.getStatus() != 2) {
            throw new BusinessException("您已经提交过实名材料，请勿重复发起");
        }

        if (exist != null) {
            // 之前被驳回：允许用户修正材料后重新提交
            exist.setRealName(data.getRealName());
            exist.setIdCard(data.getIdCard());
            exist.setIdCardFront(data.getIdCardFront());
            exist.setIdCardBack(data.getIdCardBack());
            exist.setBankName(data.getBankName());
            exist.setBankCard(data.getBankCard());
            exist.setPhone(data.getPhone());
            exist.setEmail(data.getEmail());
            exist.setStatus(0); // 重新进入待审核
            exist.setAuditTime(null);
            profileMapper.update(exist);
            return;
        }

        data.setStatus(0); // 待管理员审核
        profileMapper.insert(data);
    }

    @Override
    public UserProfile getMyProfile(Long userId) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        maskSensitiveData(profile);
        return profile;
    }

    @Override
    public List<UserProfile> getPendingKycList() {
        return maskList(profileMapper.selectPendingList());
    }

    @Override
    public List<UserProfile> getAllProfileList() {
        return maskList(profileMapper.selectAllList());
    }

    @Override
    public void auditKyc(Long adminId, Long profileId, boolean isPass) {
        UserProfile p = profileMapper.selectById(profileId);
        if (p == null || p.getStatus() != 0) return;
        
        // 1-通过，2-驳回
        p.setStatus(isPass ? 1 : 2);
        p.setAuditTime(new Date());
        profileMapper.update(p);
    }

    /**
     * 对列表中的每条档案执行敏感信息脱敏
     */
    private List<UserProfile> maskList(List<UserProfile> profiles) {
        if (profiles == null) {
            return null;
        }
        for (UserProfile profile : profiles) {
            maskSensitiveData(profile);
        }
        return profiles;
    }

    /**
     * 实名认证信息脱敏辅助方法
     */
    private void maskSensitiveData(UserProfile profile) {
        if (profile == null) {
            return;
        }
        // 身份证号脱敏：仅保留前3位与后4位，中间替换为 *
        if (profile.getIdCard() != null && profile.getIdCard().length() >= 10) {
            profile.setIdCard(profile.getIdCard().replaceAll("(?<=\\d{3})\\d(?=\\d{4})", "*"));
        }
        // 银行卡号脱敏：仅保留前4位与后4位，中间替换为 *
        if (profile.getBankCard() != null && profile.getBankCard().length() >= 8) {
            profile.setBankCard(profile.getBankCard().replaceAll("(?<=\\d{4})\\d(?=\\d{4})", "*"));
        }
        // 联系手机脱敏：保留前3位和后4位，中间替换为 *
        if (profile.getPhone() != null && profile.getPhone().length() == 11) {
            profile.setPhone(profile.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        }
    }
}
