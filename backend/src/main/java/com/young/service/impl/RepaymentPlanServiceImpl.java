package com.young.service.impl;

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
        return planMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payNormalInstallment(Long userId, Long planId, BigDecimal payAmount) {
        RepaymentPlan target = planMapper.selectByUserId(userId).stream()
                .filter(p -> p.getId().equals(planId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("还款账单不存在或无权操作"));
        
        if (target.getStatus() == 1 || target.getStatus() == 3) {
            throw new RuntimeException("此账单已处于结清状态，严禁重复还款！");
        }
        
        BigDecimal actualPayAmount = target.getTotalAmount();
        
        int originalStatus = target.getStatus();
        target.setStatus(1);
        planMapper.updateOverduePlan(target);
        
        // 生成还款流水
        RepaymentRecord r = new RepaymentRecord();
        r.setPlanId(planId);
        r.setLoanId(target.getLoanId());
        r.setUserId(userId);
        r.setPayAmount(actualPayAmount);
        r.setPayType(originalStatus == 2 ? 2 : 1);
        recordMapper.insert(r);
        
        creditMapper.unfreezeAmount(userId, target.getPrincipal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payEarlySettlement(Long userId, Long loanId) {
        LoanApplication loanApp = loanApplicationMapper.selectById(loanId);
        if (loanApp == null || !loanApp.getUserId().equals(userId)) {
            throw new RuntimeException("操作受限：该贷款单不存在或不属于当前用户！");
        }

        List<RepaymentPlan> plans = planMapper.selectByLoanId(loanId);
        BigDecimal sumTotal = BigDecimal.ZERO;
        BigDecimal sumPrincipal = BigDecimal.ZERO;
        
        for (RepaymentPlan p : plans) {
            if (p.getStatus() == 0 || p.getStatus() == 2) {
                p.setStatus(3);
                planMapper.updateOverduePlan(p);
                sumTotal = sumTotal.add(p.getTotalAmount());
                sumPrincipal = sumPrincipal.add(p.getPrincipal());
            }
        }
        
        if (sumTotal.compareTo(BigDecimal.ZERO) > 0) {
            RepaymentRecord r = new RepaymentRecord();
            r.setLoanId(loanId);
            r.setUserId(userId);
            r.setPayAmount(sumTotal);
            r.setPayType(3);
            recordMapper.insert(r);
            
            creditMapper.unfreezeAmount(userId, sumPrincipal);
        }
        
        loanApp.setStatus(3);
        loanApp.setAuditTime(new java.util.Date());
        loanApplicationMapper.updateStatus(loanApp);
    }
}
