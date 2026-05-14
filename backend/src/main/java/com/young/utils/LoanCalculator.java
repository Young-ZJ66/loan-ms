package com.young.utils;

import com.young.pojo.RepaymentPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Calendar;

@Component
public class LoanCalculator {

    public List<RepaymentPlan> generateEqualInstallmentPlan(Long loanId, Long userId, BigDecimal amount, int termMonths, BigDecimal annualRate, Date disbursalDate) {
        List<RepaymentPlan> plans = new ArrayList<>();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        
        BigDecimal temp = monthlyRate.add(BigDecimal.ONE).pow(termMonths);
        BigDecimal monthlyPayment = amount.multiply(monthlyRate).multiply(temp)
                .divide(temp.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = amount;
        
        for (int i = 1; i <= termMonths; i++) {
            RepaymentPlan plan = new RepaymentPlan();
            plan.setLoanId(loanId);
            plan.setUserId(userId);
            plan.setTermIndex(i);
            plan.setStatus(0);
            plan.setPenalty(BigDecimal.ZERO);
            
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(disbursalDate);
            calendar.add(Calendar.MONTH, i);
            plan.setDueDate(calendar.getTime());

            if (i == termMonths) {
                BigDecimal currentPrincipal = remainingPrincipal;
                BigDecimal currentInterest = monthlyPayment.subtract(currentPrincipal);
                if (currentInterest.compareTo(BigDecimal.ZERO) < 0) currentInterest = BigDecimal.ZERO;
                
                plan.setPrincipal(currentPrincipal);
                plan.setInterest(currentInterest);
                plan.setTotalAmount(currentPrincipal.add(currentInterest));
            } else {
                BigDecimal currentInterest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal currentPrincipal = monthlyPayment.subtract(currentInterest);
                
                plan.setInterest(currentInterest);
                plan.setPrincipal(currentPrincipal);
                plan.setTotalAmount(monthlyPayment);
                
                remainingPrincipal = remainingPrincipal.subtract(currentPrincipal);
            }
            plans.add(plan);
        }
        return plans;
    }

    public BigDecimal calculateTotalPenalty(BigDecimal baseAmount, int overdueDays) {
        // 单日万分之五罚息
        BigDecimal penaltyRate = new BigDecimal("0.0005");
        // 罚息总额 = 本息和 * 万分之五 * 逾期天数
        return baseAmount.multiply(penaltyRate).multiply(BigDecimal.valueOf(overdueDays)).setScale(2, RoundingMode.HALF_UP);
    }
}
