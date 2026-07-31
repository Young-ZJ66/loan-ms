package com.young.task;

import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.SysMessageMapper;
import com.young.pojo.RepaymentPlan;
import com.young.pojo.SysMessage;
import com.young.utils.LoanCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 逾期账单扫描定时任务
 */
@Component
public class OverdueScanTask {
    private static final Logger log = LoggerFactory.getLogger(OverdueScanTask.class);

    @Autowired
    private RepaymentPlanMapper planMapper;
    @Autowired
    private SysMessageMapper messageMapper;
    @Autowired
    private LoanCalculator calculator;

    /**
     * 主扫描任务：每日凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scanOverduePlans() {
        log.info("[系统跑批] 开始执行逾期账单扫描 + 即将逾期提醒...");
        LocalDate todayLocal = LocalDate.now();
        Date today = Date.from(todayLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<RepaymentPlan> overduePlans = planMapper.selectOverduePlans(today);
        for (RepaymentPlan plan : overduePlans) {
            LocalDate dueLocal = plan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long daysLate = ChronoUnit.DAYS.between(dueLocal, todayLocal);
            if (daysLate <= 0) continue;

            BigDecimal baseAmount = plan.getPrincipal().add(plan.getInterest());
            BigDecimal totalPenalty = calculator.calculateTotalPenalty(baseAmount, (int) daysLate);

            plan.setPenalty(totalPenalty);
            plan.setTotalAmount(baseAmount.add(totalPenalty));
            plan.setStatus(2);
            planMapper.updateOverduePlan(plan);
            log.info("逾期计划 ID={} 逾期{}天，重新核算罚息 {}元，最新应还 {}元",
                    plan.getId(), daysLate, totalPenalty, plan.getTotalAmount());
        }
        log.info("[逾期扫描] 本轮共处理 {} 条逾期账单", overduePlans.size());

        Date fromDate = Date.from(todayLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(todayLocal.plusDays(4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<RepaymentPlan> upcomingPlans = planMapper.selectUpcomingPlans(fromDate, toDate);

        for (RepaymentPlan plan : upcomingPlans) {
            LocalDate dueLocal = plan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long daysLeft = ChronoUnit.DAYS.between(todayLocal, dueLocal);
            if (daysLeft >= 0 && daysLeft <= 3) {
                boolean alreadySent = messageMapper.selectByUserId(plan.getUserId()).stream()
                        .anyMatch(m -> "还款温馨提醒".equals(m.getTitle())
                                && m.getContent().contains(String.format("第 %d 期还款账单", plan.getTermIndex()))
                                && m.getContent().contains(dueLocal.toString()));
                if (alreadySent) {
                    log.info("用户 {} 的第{}期还款账单（到期日 {}）已发送过到期提醒，本次跳过",
                            plan.getUserId(), plan.getTermIndex(), dueLocal);
                    continue;
                }

                SysMessage msg = new SysMessage();
                msg.setToUserId(plan.getUserId());
                msg.setTitle("还款温馨提醒");
                msg.setContent(String.format(
                        "您的第 %d 期还款账单（应还金额 %.2f 元）将于 %s 到期，还有 %d 天，请尽快安排还款，逾期将产生万五日息罚金！",
                        plan.getTermIndex(), plan.getTotalAmount(), dueLocal, daysLeft
                ));
                messageMapper.insert(msg);
                log.info("已向用户 {} 推送第{}期即将逾期提醒，距到期还有{}天", plan.getUserId(), plan.getTermIndex(), daysLeft);
            }
        }
        log.info("[到期提醒] 本轮共扫描 {} 条即将到期待还账单", upcomingPlans.size());
    }

    /**
     * 手动触发逾期扫描
     */
    public void triggerManually() {
        scanOverduePlans();
    }
}
