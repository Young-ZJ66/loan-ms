package com.young.mapper;

import com.young.pojo.RepaymentPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface RepaymentPlanMapper {
    int insert(RepaymentPlan plan);
    List<RepaymentPlan> selectByUserId(Long userId);
    List<RepaymentPlan> selectByLoanId(Long loanId);
    List<RepaymentPlan> selectOverduePlans(Date today);
    int updateOverduePlan(RepaymentPlan plan);

    /** 按主键查询单条还款计划 */
    RepaymentPlan selectById(Long id);

    /** 原子结清单期账单：仅当状态为待还(0)或逾期(2)时置为已还(1)，返回受影响行数（防并发重复还款） */
    int settlePlan(@Param("id") Long id);

    /** 原子标记提前结清：仅当状态为待还(0)或逾期(2)时置为结清(3)，返回受影响行数 */
    int settleEarly(@Param("id") Long id);

    /** 【管理端】查询全平台所有逾期账单（含客户姓名） */
    List<RepaymentPlan> selectOverdueAll();

    /** 统计逾期账单数量 */
    @Select("SELECT COUNT(*) FROM repayment_plan WHERE status = 2")
    int countOverdue();

    /** 查询待还且即将到期的账单（用于到期提醒推送） */
    List<RepaymentPlan> selectUpcomingPlans(@Param("fromDate") Date fromDate, @Param("toDate") Date toDate);
    
    /** 【管理端】查询全平台所有还款计划（含客户姓名通过 remark 字段回传） */
    List<RepaymentPlan> selectAll();
    
    /** 统计逾期坏账总金额（status=2 即逾期未还） */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM repayment_plan WHERE status = 2")
    java.math.BigDecimal sumOverdueAmount();

    /** 统计用户当前真实且精准的信贷结余占用（未结清的本金总和） */
    @Select("SELECT COALESCE(SUM(principal), 0) FROM repayment_plan WHERE user_id = #{userId} AND status IN (0, 2)")
    java.math.BigDecimal sumUnpaidPrincipal(Long userId);
}
