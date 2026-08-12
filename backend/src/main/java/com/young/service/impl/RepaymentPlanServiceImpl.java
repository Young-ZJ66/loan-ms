package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.RepaymentRecordMapper;
import com.young.mapper.UserCreditMapper;
import com.young.mapper.LoanApplicationMapper;
import com.young.pojo.RepaymentPlan;
import com.young.pojo.RepaymentRecord;
import com.young.pojo.LoanApplication;
import com.young.service.RepaymentPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class RepaymentPlanServiceImpl implements RepaymentPlanService {

    @Autowired
    private RepaymentPlanMapper planMapper;
    @Autowired
    private RepaymentRecordMapper recordMapper;
    @Autowired
    private UserCreditMapper creditMapper;
    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Override
    public List<RepaymentPlan> getUserPlans(Long userId, Integer status) {
        return planMapper.selectByUserId(userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payNormalInstallment(Long userId, Long planId, BigDecimal payAmount) {
        RepaymentPlan target = planMapper.selectById(planId);
        if (target == null || !target.getUserId().equals(userId)) {
            throw new BusinessException("还款账单不存在或无权操作");
        }

        if (target.getStatus() == 1 || target.getStatus() == 3) {
            throw new BusinessException("此账单已处于结清状态，严禁重复还款！");
        }

        // 原子结清：并发下仅有一个请求能成功
        int affected = planMapper.settlePlan(planId);
        if (affected == 0) {
            throw new BusinessException("账单状态已变化，请刷新后重试");
        }

        BigDecimal actualPayAmount = target.getTotalAmount();
        int originalStatus = target.getStatus();

        // 生成还款流水
        RepaymentRecord r = new RepaymentRecord();
        r.setPlanId(planId);
        r.setLoanId(target.getLoanId());
        r.setUserId(userId);
        r.setPayAmount(actualPayAmount);
        r.setPayType(originalStatus == 2 ? 2 : 1);
        recordMapper.insert(r);

        creditMapper.unfreezeAmount(userId, target.getPrincipal());

        // 检查该贷款下所有账单是否均已结清，若是则同步贷款状态为已结清
        markLoanSettledIfAllCleared(target.getLoanId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payEarlySettlement(Long userId, Long loanId) {
        LoanApplication loanApp = loanApplicationMapper.selectById(loanId);
        if (loanApp == null || !loanApp.getUserId().equals(userId)) {
            throw new BusinessException("操作受限：该贷款单不存在或不属于当前用户！");
        }
        // 仅已放款(1)状态的贷款允许提前结清
        if (loanApp.getStatus() == null || loanApp.getStatus() != 1) {
            throw new BusinessException("当前贷款状态不支持提前结清");
        }

        List<RepaymentPlan> plans = planMapper.selectByLoanId(loanId);
        BigDecimal sumTotal = BigDecimal.ZERO;
        BigDecimal sumPrincipal = BigDecimal.ZERO;

        for (RepaymentPlan p : plans) {
            if (p.getStatus() == 0 || p.getStatus() == 2) {
                // 原子标记结清，防并发重复结清
                int affected = planMapper.settleEarly(p.getId());
                if (affected == 0) {
                    continue;
                }
                sumTotal = sumTotal.add(p.getTotalAmount());
                sumPrincipal = sumPrincipal.add(p.getPrincipal());
            }
        }

        // 仅当确实有账单被结清时，才生成流水与更新贷款状态
        if (sumTotal.compareTo(BigDecimal.ZERO) > 0) {
            RepaymentRecord r = new RepaymentRecord();
            r.setLoanId(loanId);
            r.setUserId(userId);
            r.setPayAmount(sumTotal);
            r.setPayType(3);
            recordMapper.insert(r);

            creditMapper.unfreezeAmount(userId, sumPrincipal);

            loanApp.setStatus(3);
            loanApp.setAuditTime(new Date());
            loanApplicationMapper.updateStatus(loanApp);
        }
    }

    /**
     * 检查指定贷款下是否所有还款计划均已结清（status IN (1, 3)），
     * 若是则将贷款申请状态更新为已结清(3)
     */
    private void markLoanSettledIfAllCleared(Long loanId) {
        List<RepaymentPlan> plans = planMapper.selectByLoanId(loanId);
        if (plans == null || plans.isEmpty()) {
            return;
        }
        for (RepaymentPlan p : plans) {
            if (p.getStatus() == null || p.getStatus() == 0 || p.getStatus() == 2) {
                return;
            }
        }
        LoanApplication loanApp = loanApplicationMapper.selectById(loanId);
        if (loanApp != null && loanApp.getStatus() != null && loanApp.getStatus() == 1) {
            loanApp.setStatus(3);
            loanApp.setAuditTime(new Date());
            loanApplicationMapper.updateStatus(loanApp);
        }
    }
}
