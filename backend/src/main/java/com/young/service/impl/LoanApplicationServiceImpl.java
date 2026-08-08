package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.LoanApplicationMapper;
import com.young.mapper.LoanProductMapper;
import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.UserCreditMapper;
import com.young.mapper.UserProfileMapper;
import com.young.pojo.LoanApplication;
import com.young.pojo.LoanProduct;
import com.young.pojo.RepaymentPlan;
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
            throw new BusinessException("请先完成实名认证并获得审批通过，方可发起贷款申请！");
        }

        // 金额与期限基础校验
        if (app.getAmount() == null || app.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("贷款金额必须大于0");
        }
        if (app.getTermMonths() == null || app.getTermMonths() <= 0) {
            throw new BusinessException("贷款期限必须大于0");
        }

        // 必须关联上架中的贷款产品，利率等核心参数一律以产品配置为准，不信任客户端提交值
        if (app.getProductId() == null) {
            throw new BusinessException("必须选择贷款产品");
        }
        LoanProduct product = productMapper.selectById(app.getProductId());
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("所选贷款产品不存在或已下架");
        }
        if (app.getAmount().compareTo(product.getMinAmount()) < 0
                || app.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException("申请金额超出产品允许范围");
        }
        if (app.getTermMonths() < product.getMinTerm()
                || app.getTermMonths() > product.getMaxTerm()) {
            throw new BusinessException("申请期限超出产品允许范围");
        }

        int updated = creditMapper.freezeAmount(userId, app.getAmount());
        if (updated == 0) {
            throw new BusinessException("操作被拦截：当前可用信用额度不足，或您的账户已被风控冻结！");
        }

        app.setUserId(userId);
        app.setStatus(0);
        app.setApplyTime(new Date());
        app.setAnnualRate(product.getAnnualRate());

        applicationMapper.insert(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAndDisburse(Long applicationId) {
        LoanApplication app = applicationMapper.selectById(applicationId);
        if (app == null || app.getStatus() != 0) {
            throw new BusinessException("审批流异常：找不到该申请或状态不在待审态");
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

    @Override
    public List<LoanApplication> getPendingList() {
        return applicationMapper.selectPending();
    }
}
