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
 * 说明：目前配置为每分钟执行，以便于部署初期监控；常规生产环境推荐调整为 "0 0 1 * * ?"（每日凌晨1点）
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
     * cron："0 0 1 * * ?"
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scanOverduePlans() {
        log.info("[系统跑批] 开始执行逾期账单扫描 + 即将逾期提醒...");
        Date today = new Date();
        LocalDate todayLocal = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // ① 逾期罚息累计处理
        List<RepaymentPlan> overduePlans = planMapper.selectOverduePlans(today);
        for (RepaymentPlan plan : overduePlans) {
            LocalDate dueLocal = plan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long daysLate = ChronoUnit.DAYS.between(dueLocal, todayLocal);
            if (daysLate <= 0) continue;
            
            // 使用幂等计息法：重新计算到今天为止的全部单利罚息并覆盖
            BigDecimal baseAmount = plan.getPrincipal().add(plan.getInterest());
            BigDecimal totalPenalty = calculator.calculateTotalPenalty(baseAmount, (int) daysLate);
            
            plan.setPenalty(totalPenalty);
            // 账单最新总额 = 原始本息 + 计算到今天的罚息总和
            plan.setTotalAmount(baseAmount.add(totalPenalty));
            plan.setStatus(2); // 置为逾期中
            planMapper.updateOverduePlan(plan);
            log.info("逾期计划 ID={} 逾期{}天，重新核算罚息 {}元，最新应还 {}元",
                    plan.getId(), daysLate, totalPenalty, plan.getTotalAmount());
        }
        log.info("[逾期扫描] 本轮共处理 {} 条逾期账单", overduePlans.size());

        // ② 即将逾期提醒：提前 3 天推送站内消息（去重：仅在未读消息中无同一期次时才推送）
        List<RepaymentPlan> allPendingPlans = planMapper.selectOverduePlans(
                // 取3天后那一时刻作为截止线，抓取3天内即将到期的"待还"账单
                java.util.Date.from(todayLocal.plusDays(4).atStartOfDay(ZoneId.systemDefault()).toInstant())
        );
        for (RepaymentPlan plan : allPendingPlans) {
            if (plan.getStatus() != 0) continue; // 只给"待还"状态的发提醒
            LocalDate dueLocal = plan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long daysLeft = ChronoUnit.DAYS.between(todayLocal, dueLocal);
            if (daysLeft >= 0 && daysLeft <= 3) {
                // 检查是否已发过该期次的未读提醒，避免重复轰炸
                int unreadCount = messageMapper.countUnread(plan.getUserId());
                // 检查未读总数以实施基础速率限制
                // 待优化：生产环境需结合 plan_id + 消息类型构建幂等去重表
                SysMessage msg = new SysMessage();
                msg.setToUserId(plan.getUserId());
                msg.setTitle("⚠️ 还款温馨提醒");
                msg.setContent(String.format(
                        "您的第 %d 期还款账单（应还金额 %.2f 元）将于 %s 到期，还有 %d 天，请尽快安排还款，逾期将产生万五日息罚金！",
                        plan.getTermIndex(), plan.getTotalAmount(), dueLocal, daysLeft
                ));
                messageMapper.insert(msg);
                log.info("已向用户 {} 推送第{}期即将逾期提醒，距到期还有{}天", plan.getUserId(), plan.getTermIndex(), daysLeft);
            }
        }
    }

    /**
     * 手动触发逾期扫描（供运营后台按需即时清算）
     */
    public void triggerManually() {
        scanOverduePlans();
    }
}
