package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.CreditApplicationMapper;
import com.young.mapper.UserCreditMapper;
import com.young.mapper.UserProfileMapper;
import com.young.pojo.CreditApplication;
import com.young.pojo.UserCredit;
import com.young.pojo.UserProfile;
import com.young.service.CreditApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreditApplicationServiceImpl implements CreditApplicationService {

    @Autowired
    private CreditApplicationMapper applicationMapper;
    @Autowired
    private UserCreditMapper userCreditMapper;
    @Autowired
    private UserProfileMapper userProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("申请额度必须大于0");
        }
        // 先检查是否实名认证通过
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null || profile.getStatus() != 1) {
            throw new BusinessException("操作受限：必须先完成实名认证且经管理员审核通过后，才能发起额度申请！");
        }

        // 检查是否有在途申请
        CreditApplication pending = applicationMapper.selectLatestPendingByUserId(userId);
        if (pending != null) {
            throw new BusinessException("您有一笔申请正在火速核批中，请勿重复提交申请！");
        }

        // 检查是否已被冻结额度
        UserCredit existCredit = userCreditMapper.selectByUserId(userId);
        if (existCredit != null && existCredit.getStatus() == 0) {
            throw new BusinessException("账户已被风控中心冻结，拒绝受理提额申请！");
        }

        CreditApplication app = new CreditApplication();
        app.setUserId(userId);
        app.setRequestedAmount(amount);
        app.setStatus(0); // 待审
        applicationMapper.insert(app);
    }

    @Override
    public CreditApplication getMyLatestPending(Long userId) {
        return applicationMapper.selectLatestPendingByUserId(userId);
    }

    @Override
    public List<CreditApplication> getPendingList() {
        return applicationMapper.selectPendingList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long applicationId, BigDecimal approveAmount, Long adminId) {
        if (approveAmount == null || approveAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("下发额度必须大于0");
        }

        CreditApplication app = applicationMapper.selectById(applicationId);
        if (app == null || app.getStatus() != 0) {
            throw new BusinessException("找不到该申请或状态不在待审态");
        }

        app.setStatus(1);
        app.setAuditTime(LocalDateTime.now());
        // CAS 抢占审批权
        int affected = applicationMapper.updateStatusIfPending(app);
        if (affected == 0) {
            throw new BusinessException("该申请已被其他管理员处理，请刷新列表");
        }

        UserCredit existCredit = userCreditMapper.selectByUserId(app.getUserId());
        if (existCredit == null) {
            // 首次建额
            UserCredit userCredit = new UserCredit();
            userCredit.setUserId(app.getUserId());
            userCredit.setTotalCredit(approveAmount);
            userCredit.setAvailableCredit(approveAmount);
            userCredit.setUsedCredit(BigDecimal.ZERO);
            userCredit.setStatus(1); // 1-正常
            userCreditMapper.insert(userCredit);
        } else {
            // 后续提额/降额：校验不得低于当前已用额度，保持 total = available + used 不变量
            if (approveAmount.compareTo(existCredit.getUsedCredit()) < 0) {
                throw new BusinessException("批复额度不得低于当前已用额度 " + existCredit.getUsedCredit() + " 元");
            }
            BigDecimal diff = approveAmount.subtract(existCredit.getTotalCredit());
            existCredit.setTotalCredit(approveAmount);
            BigDecimal newAvailable = existCredit.getAvailableCredit().add(diff);
            existCredit.setAvailableCredit(newAvailable);
            userCreditMapper.updateCredit(existCredit);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long applicationId, Long adminId) {
        CreditApplication app = applicationMapper.selectById(applicationId);
        if (app == null || app.getStatus() != 0) {
            throw new BusinessException("申请不存在或状态已变更，无法驳回");
        }

        app.setStatus(2);
        app.setAuditTime(LocalDateTime.now());
        int affected = applicationMapper.updateStatusIfPending(app);
        if (affected == 0) {
            throw new BusinessException("该申请已被其他管理员处理，请刷新列表");
        }
    }
}
