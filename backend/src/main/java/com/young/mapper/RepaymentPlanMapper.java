package com.young.mapper;

import com.young.pojo.RepaymentPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface RepaymentPlanMapper {
    int insert(RepaymentPlan plan);
    List<RepaymentPlan> selectByUserId(Long userId);
    List<RepaymentPlan> selectByLoanId(Long loanId);
    List<RepaymentPlan> selectOverduePlans(Date today);
    int updateOverduePlan(RepaymentPlan plan);
    
    /** 【管理端】查询全平台所有还款计划（含客户姓名通过 remark 字段回传） */
    List<RepaymentPlan> selectAll();
    
    /** 统计逾期坏账总金额（status=2 即逾期未还） */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM repayment_plan WHERE status = 2")
    java.math.BigDecimal sumOverdueAmount();

    /** 统计用户当前真实且精准的信贷结余占用（未结清的本金总和） */
    @Select("SELECT COALESCE(SUM(principal), 0) FROM repayment_plan WHERE user_id = #{userId} AND status IN (0, 2)")
    java.math.BigDecimal sumUnpaidPrincipal(Long userId);
}
