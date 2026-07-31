package com.young.service.impl;

import com.young.mapper.LoanApplicationMapper;
import com.young.mapper.LoanProductMapper;
import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.UserCreditMapper;
import com.young.mapper.UserProfileMapper;
import com.young.pojo.LoanApplication;
import com.young.pojo.LoanProduct;
import com.young.pojo.RepaymentPlan;
import com.young.pojo.UserCredit;
import com.young.pojo.UserProfile;
import com.young.service.LoanApplicationService;
import com.young.utils.LoanCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    @Autowired
    private LoanApplicationMapper applicationMapper;
    @Autowired
    private UserCreditMapper creditMapper;
    @Autowired
    private UserProfileMapper userProfileMapper;
    @Autowired
    private RepaymentPlanMapper planMapper;
    @Autowired
    private LoanCalculator calculator;
    @Autowired
    private LoanProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyLoan(Long userId, LoanApplication app) {
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null || profile.getStatus() != 1) {
            throw new RuntimeException("请先完成实名认证并获得审批通过，方可发起贷款申请！");
        }

        int updated = creditMapper.freezeAmount(userId, app.getAmount());
        if (updated == 0) {
            throw new RuntimeException("操作被拦截：当前可用信用额度不足，或您的账户已被风控冻结！");
        }

        app.setUserId(userId);
        app.setStatus(0);
        app.setApplyTime(new Date());
        
        if (app.getProductId() != null) {
            LoanProduct product = productMapper.selectById(app.getProductId());
            if (product != null) {
                app.setAnnualRate(product.getAnnualRate());
            }
        }

        applicationMapper.insert(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAndDisburse(Long applicationId) {
        LoanApplication app = applicationMapper.selectById(applicationId);
        if (app == null || app.getStatus() != 0) {
            throw new RuntimeException("审批流异常：找不到该申请或状态不在待审态");
        }

        app.setStatus(1);
        app.setAuditTime(new Date());
        applicationMapper.updateStatus(app);

        // 生成还款计划
        List<RepaymentPlan> plans = calculator.generateEqualInstallmentPlan(
                app.getId(),
                app.getUserId(),
                app.getAmount(),
                app.getTermMonths(),
                app.getAnnualRate() != null ? app.getAnnualRate() : new BigDecimal("0.048"),
                new Date());
        for (RepaymentPlan plan : plans) {
            planMapper.insert(plan);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectLoan(Long applicationId) {
        LoanApplication app = applicationMapper.selectById(applicationId);
        if (app == null || app.getStatus() != 0)
            return;

        app.setStatus(2);
        app.setAuditTime(new Date());
        applicationMapper.updateStatus(app);

        creditMapper.unfreezeAmount(app.getUserId(), app.getAmount());
    }

    @Override
    public List<LoanApplication> getApplicationList(Long userId) {
        return applicationMapper.selectList(userId);
    }
}
