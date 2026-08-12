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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // 单机运行状态锁，防止 @Scheduled 与手动触发并发执行（多实例部署需替换为分布式锁）
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 主扫描任务：每日凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scanOverduePlans() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[系统跑批] 逾期扫描任务已在运行，本次跳过");
            return;
        }
        try {
            doScan();
        } finally {
            running.set(false);
        }
    }

    private void doScan() {
        log.info("[系统跑批] 开始执行逾期账单扫描 + 即将逾期提醒...");
        LocalDate todayLocal = LocalDate.now();
        Date today = Date.from(todayLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<RepaymentPlan> overduePlans = planMapper.selectOverduePlans(today);
        int processed = 0;
        for (RepaymentPlan plan : overduePlans) {
            // 异常隔离：单条 plan 处理失败不影响后续
            try {
                if (plan.getDueDate() == null) {
                    log.warn("[系统跑批] 跳过缺失到期日的还款计划 ID={}", plan.getId());
                    continue;
                }
                LocalDate dueLocal = plan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                long daysLate = ChronoUnit.DAYS.between(dueLocal, todayLocal);
                if (daysLate <= 0) continue;

                BigDecimal baseAmount = plan.getPrincipal().add(plan.getInterest());
                BigDecimal totalPenalty = calculator.calculateTotalPenalty(baseAmount, (int) daysLate);

                plan.setPenalty(totalPenalty);
                plan.setTotalAmount(baseAmount.add(totalPenalty));
                plan.setStatus(2);
                // updateOverduePlan SQL 已带 status IN (0,2) 条件，并发安全
                planMapper.updateOverduePlan(plan);
                processed++;
            } catch (Exception e) {
                log.error("[系统跑批] 处理逾期计划 ID={} 失败", plan.getId(), e);
            }
        }
        log.info("[逾期扫描] 本轮共处理 {} 条逾期账单", processed);

        // 即将到期提醒：查询 due_date 在 (今天, 今天+4天) 范围内的待还账单
        Date fromDate = today;
        Date toDate = Date.from(todayLocal.plusDays(4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<RepaymentPlan> upcomingPlans = planMapper.selectUpcomingPlans(fromDate, toDate);
        int reminded = 0;
        for (RepaymentPlan plan : upcomingPlans) {
            try {
                if (plan.getDueDate() == null) continue;
                LocalDate dueLocal = plan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                long daysLeft = ChronoUnit.DAYS.between(todayLocal, dueLocal);
                if (daysLeft >= 1 && daysLeft <= 3) {
                    boolean alreadySent = messageMapper.countReminder(plan.getUserId(), plan.getTermIndex(), dueLocal.toString()) > 0;
                    if (alreadySent) {
                        continue;
                    }
                    String totalAmountStr = plan.getTotalAmount() != null
                            ? plan.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                            : "0.00";
                    SysMessage msg = new SysMessage();
                    msg.setToUserId(plan.getUserId());
                    msg.setTitle("还款温馨提醒");
                    msg.setContent("您的第 " + plan.getTermIndex() + " 期还款账单（应还金额 " + totalAmountStr
                            + " 元）将于 " + dueLocal + " 到期，还有 " + daysLeft + " 天，请尽快安排还款，逾期将产生万五日息罚金！");
                    messageMapper.insert(msg);
                    reminded++;
                }
            } catch (Exception e) {
                log.error("[系统跑批] 处理到期提醒计划 ID={} 失败", plan.getId(), e);
            }
        }
        log.info("[到期提醒] 本轮共推送 {} 条即将到期提醒（扫描 {} 条）", reminded, upcomingPlans.size());
    }

    /**
     * 手动触发逾期扫描
     */
    public void triggerManually() {
        scanOverduePlans();
    }
}
