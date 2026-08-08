package com.young;

import com.young.pojo.RepaymentPlan;
import com.young.utils.LoanCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanCalculatorTest {

    private final LoanCalculator calculator = new LoanCalculator();

    @Test
    void generateEqualInstallmentPlan_producesBalancedSchedules() {
        List<RepaymentPlan> plans = calculator.generateEqualInstallmentPlan(
                1L, 2L, new BigDecimal("10000"), 12, new BigDecimal("0.048"), new Date());

        assertEquals(12, plans.size());

        // 各期应还总额应保持一致（末期允许少量尾差，容差 0.02）
        BigDecimal firstPayment = plans.get(0).getTotalAmount();
        for (RepaymentPlan p : plans) {
            assertDiffAtMost(p.getTotalAmount(), firstPayment, new BigDecimal("0.02"));
        }

        // 各期本金之和应等于贷款本金（容差 0.02）
        BigDecimal totalPrincipal = plans.stream()
                .map(RepaymentPlan::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertDiffAtMost(totalPrincipal, new BigDecimal("10000"), new BigDecimal("0.02"));

        // 期数顺序与到期日递增
        for (int i = 1; i <= plans.size(); i++) {
            assertEquals(i, plans.get(i - 1).getTermIndex());
        }
    }

    private void assertDiffAtMost(BigDecimal actual, BigDecimal expected, BigDecimal tolerance) {
        assertTrue(actual.subtract(expected).abs().compareTo(tolerance) <= 0,
                "期望 " + expected + "，实际 " + actual + "，偏差超出容差 " + tolerance);
    }

    @Test
    void generateEqualInstallmentPlan_handlesSingleTerm() {
        List<RepaymentPlan> plans = calculator.generateEqualInstallmentPlan(
                1L, 1L, new BigDecimal("1000"), 1, new BigDecimal("0.048"), new Date());

        assertEquals(1, plans.size());
        RepaymentPlan plan = plans.get(0);
        assertTrue(plan.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);
        // 末期本金应等于剩余全部本金
        assertEquals(0, plan.getPrincipal().compareTo(new BigDecimal("1000")));
    }

    @Test
    void calculateTotalPenalty_zeroDaysIsZero() {
        assertEquals(0, calculator.calculateTotalPenalty(new BigDecimal("1000"), 0).compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateTotalPenalty_matchesFiveBpsPerDay() {
        // 1000 * 0.0005 * 10 = 5.00
        assertEquals(0, calculator.calculateTotalPenalty(new BigDecimal("1000"), 10).compareTo(new BigDecimal("5.00")));
    }
}
