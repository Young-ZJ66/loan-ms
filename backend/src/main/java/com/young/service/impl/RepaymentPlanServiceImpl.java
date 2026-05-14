package com.young.service.impl;

import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.RepaymentRecordMapper;
import com.young.mapper.UserCreditMapper;
import com.young.pojo.RepaymentPlan;
import com.young.pojo.RepaymentRecord;
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

    @Override
    public List<RepaymentPlan> getUserPlans(Long userId, Integer status) {
        // 原生 MyBatis 已在 RepaymentPlanMapper 实现
        return planMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payNormalInstallment(Long userId, Long planId, BigDecimal payAmount) {
        // 先通过 selectByUserId 找到该计划，确保 penalty 字段有值
        RepaymentPlan target = planMapper.selectByUserId(userId).stream()
                .filter(p -> p.getId().equals(planId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("还款账单不存在或无权操作"));
        
        // 并发及防重保护：已经结清的禁止重复还款
        if (target.getStatus() == 1 || target.getStatus() == 3) {
            throw new RuntimeException("此账单已处于结清状态，严禁重复还款！");
        }
        
        // 先记录原始逾期状态，再翻转为已结清
        int originalStatus = target.getStatus();
        target.setStatus(1);  // 1-已结清
        target.setTotalAmount(payAmount);
        planMapper.updateOverduePlan(target);
        
        // 生成还款流水
        RepaymentRecord r = new RepaymentRecord();
        r.setPlanId(planId);
        r.setLoanId(target.getLoanId());
        r.setUserId(userId);
        r.setPayAmount(payAmount);
        // 原为逾期中则 payType=2（逾期清欠），否则 payType=1（正常按期还款）
        r.setPayType(originalStatus == 2 ? 2 : 1);
        recordMapper.insert(r);
        
        // 安全拦截：只恢复占用的本金，决不能将利息和罚息加给可用额度
        creditMapper.unfreezeAmount(userId, target.getPrincipal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payEarlySettlement(Long userId, Long loanId) {
        List<RepaymentPlan> plans = planMapper.selectByLoanId(loanId);
        BigDecimal sumTotal = BigDecimal.ZERO;
        BigDecimal sumPrincipal = BigDecimal.ZERO;
        
        for (RepaymentPlan p : plans) {
            if (p.getStatus() == 0 || p.getStatus() == 2) {
                p.setStatus(3); // 3-提前结清
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
            r.setPayType(3); // 3-提前结清
            recordMapper.insert(r);
            
            // 同样：恢复授信只需要恢复本金
            creditMapper.unfreezeAmount(userId, sumPrincipal);
        }
    }
}
