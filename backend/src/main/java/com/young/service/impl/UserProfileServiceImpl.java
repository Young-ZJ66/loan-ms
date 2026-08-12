package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.UserProfileMapper;
import com.young.pojo.UserProfile;
import com.young.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    @Autowired
    private UserProfileMapper profileMapper;

    @Override
    public void submitKyc(UserProfile data) {
        if (containsMask(data.getIdCard()) || containsMask(data.getBankCard()) || containsMask(data.getPhone())) {
            throw new BusinessException("请填写完整、真实的证件与联系信息");
        }

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
        // 本人查询返回真实数据，便于被驳回后直接沿用原资料重新提交
        return profileMapper.selectByUserId(userId);
    }

    @Override
    public List<UserProfile> getPendingKycList() {
        // 管理员审批需要查看完整资料，后端不脱敏，由前端控制展示粒度
        return profileMapper.selectPendingList();
    }

    @Override
    public List<UserProfile> getAllProfileList() {
        return profileMapper.selectAllList();
    }

    @Override
    public void auditKyc(Long adminId, Long profileId, boolean isPass) {
        UserProfile p = profileMapper.selectById(profileId);
        if (p == null || p.getStatus() != 0) {
            throw new BusinessException("档案不存在或状态已变更，无法审批");
        }

        // 1-通过，2-驳回
        p.setStatus(isPass ? 1 : 2);
        p.setAuditTime(new Date());
        profileMapper.update(p);
        log.info("[KYC审批] 管理员 {} 审批档案 ID={}，结果={}", adminId, profileId, isPass ? "通过" : "驳回");
    }

    /**
     * 判定敏感字段是否仍是脱敏值（含 *），防止掩码被写入数据库
     */
    private boolean containsMask(String value) {
        return value != null && value.contains("*");
    }
}
