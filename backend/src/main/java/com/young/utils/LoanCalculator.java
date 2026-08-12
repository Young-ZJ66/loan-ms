package com.young.utils;

import com.young.common.BusinessException;
import com.young.pojo.RepaymentPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class LoanCalculator {

    // 罚息年化上限：单利 24%（司法保护上限参考），按日计息
    private static final BigDecimal PENALTY_DAILY_RATE = new BigDecimal("0.0005");
    private static final BigDecimal PENALTY_CAP_RATIO = new BigDecimal("0.24");

    public List<RepaymentPlan> generateEqualInstallmentPlan(Long loanId, Long userId, BigDecimal amount, int termMonths, BigDecimal annualRate, Date disbursalDate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("贷款金额必须大于0");
        }
        if (termMonths <= 0) {
            throw new BusinessException("贷款期数必须大于0");
        }
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("年化利率不能为空或为负");
        }
        if (disbursalDate == null) {
            throw new BusinessException("放款日期不能为空");
        }

        List<RepaymentPlan> plans = new ArrayList<>();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        // 利率为 0 时走等额本金：每期本金 = amount / termMonths，利息 = 0
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal perPrincipal = amount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
            BigDecimal remainingPrincipal = amount;
            for (int i = 1; i <= termMonths; i++) {
                RepaymentPlan plan = new RepaymentPlan();
                plan.setLoanId(loanId);
                plan.setUserId(userId);
                plan.setTermIndex(i);
                plan.setStatus(0);
                plan.setPenalty(BigDecimal.ZERO);
                plan.setDueDate(addMonths(disbursalDate, i));
                if (i == termMonths) {
                    // 末期为消除累计误差，直接取剩余本金
                    plan.setPrincipal(remainingPrincipal);
                } else {
                    plan.setPrincipal(perPrincipal);
                    remainingPrincipal = remainingPrincipal.subtract(perPrincipal);
                }
                plan.setInterest(BigDecimal.ZERO);
                plan.setTotalAmount(plan.getPrincipal());
                plans.add(plan);
            }
            return plans;
        }

        // 中间计算全程保留 10 位小数，避免精度累计误差，仅在最终展示字段 setScale(2)
        BigDecimal temp = monthlyRate.add(BigDecimal.ONE).pow(termMonths);
        BigDecimal monthlyPayment = amount.multiply(monthlyRate).multiply(temp)
                .divide(temp.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = amount;

        for (int i = 1; i <= termMonths; i++) {
            RepaymentPlan plan = new RepaymentPlan();
            plan.setLoanId(loanId);
            plan.setUserId(userId);
            plan.setTermIndex(i);
            plan.setStatus(0);
            plan.setPenalty(BigDecimal.ZERO);
            plan.setDueDate(addMonths(disbursalDate, i));

            // 用高精度计算利息，避免前期 setScale 导致末期本金跳变
            BigDecimal currentInterestHigh = remainingPrincipal.multiply(monthlyRate);
            BigDecimal currentPrincipalHigh;
            if (i == termMonths) {
                // 末期本金取剩余本金，利息用月供反推，避免负利息
                currentPrincipalHigh = remainingPrincipal;
                BigDecimal currentInterest = monthlyPayment.subtract(currentPrincipalHigh);
                if (currentInterest.compareTo(BigDecimal.ZERO) < 0) {
                    currentInterest = BigDecimal.ZERO;
                }
                plan.setPrincipal(currentPrincipalHigh.setScale(2, RoundingMode.HALF_UP));
                plan.setInterest(currentInterest.setScale(2, RoundingMode.HALF_UP));
                plan.setTotalAmount(plan.getPrincipal().add(plan.getInterest()));
            } else {
                BigDecimal currentInterest = currentInterestHigh.setScale(2, RoundingMode.HALF_UP);
                BigDecimal currentPrincipal = monthlyPayment.subtract(currentInterestHigh).setScale(2, RoundingMode.HALF_UP);
                plan.setInterest(currentInterest);
                plan.setPrincipal(currentPrincipal);
                plan.setTotalAmount(plan.getPrincipal().add(plan.getInterest()));
                remainingPrincipal = remainingPrincipal.subtract(currentPrincipal);
            }
            plans.add(plan);
        }
        return plans;
    }

    public BigDecimal calculateTotalPenalty(BigDecimal baseAmount, int overdueDays) {
        if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0 || overdueDays <= 0) {
            return BigDecimal.ZERO;
        }
        // 罚息 = 本息和 * 万分之五 * 逾期天数
        BigDecimal penalty = baseAmount.multiply(PENALTY_DAILY_RATE).multiply(BigDecimal.valueOf(overdueDays));
        // 罚息上限：不超过本金的 24%，避免长期逾期罚息远超本金（合规风险）
        BigDecimal cap = baseAmount.multiply(PENALTY_CAP_RATIO);
        if (penalty.compareTo(cap) > 0) {
            penalty = cap;
        }
        return penalty.setScale(2, RoundingMode.HALF_UP);
    }

    private Date addMonths(Date date, int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, months);
        return calendar.getTime();
    }
}
