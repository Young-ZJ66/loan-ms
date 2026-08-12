package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.UnfreezeApplicationMapper;
import com.young.mapper.UserCreditMapper;
import com.young.pojo.UnfreezeApplication;
import com.young.pojo.UserCredit;
import com.young.service.UnfreezeApplicationService;
import com.young.service.UserCreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 解冻申诉服务实现
 */
@Service
public class UnfreezeApplicationServiceImpl implements UnfreezeApplicationService {

    @Autowired
    private UnfreezeApplicationMapper applicationMapper;
    @Autowired
    private UserCreditService creditService;
    @Autowired
    private UserCreditMapper userCreditMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyUnfreeze(Long userId, String reason) {
        // 仅当账户确为冻结状态时才允许发起解冻申诉
        UserCredit credit = userCreditMapper.selectByUserId(userId);
        if (credit == null) {
            throw new BusinessException("您当前无授信账户，无需解冻");
        }
        if (credit.getStatus() != 0) {
            throw new BusinessException("您的账户未被冻结，无需提交解冻申诉");
        }

        UnfreezeApplication pending = applicationMapper.selectLatestPendingByUserId(userId);
        if (pending != null) {
            throw new BusinessException("您已有正在被审查中的解冻申诉，不可重复提交");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("解冻申诉理由不能为空");
        }
        UnfreezeApplication app = new UnfreezeApplication();
        app.setUserId(userId);
        app.setReason(reason);
        app.setStatus(0);
        applicationMapper.insert(app);
    }

    @Override
    public UnfreezeApplication getMyPending(Long userId) {
        return applicationMapper.selectLatestPendingByUserId(userId);
    }

    @Override
    public List<UnfreezeApplication> listAll() {
        return applicationMapper.selectList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, boolean isPass, Long adminId) {
        UnfreezeApplication app = applicationMapper.selectById(id);
        if (app == null || app.getStatus() != 0) {
            throw new BusinessException("非法审批：工单不存在或状态已变更");
        }
        app.setStatus(isPass ? 1 : 2);
        app.setAdminId(adminId);
        applicationMapper.update(app);

        if (isPass) {
            // 解冻动作由本方法记录的 adminId 审计，unfreezeCreditAuth 仅负责状态切换与通知
            creditService.unfreezeCreditAuth(app.getUserId(), adminId);
        }
    }
}
