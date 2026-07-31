package com.young.mapper;

import com.young.pojo.LoanApplication;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface LoanApplicationMapper {
    int insert(LoanApplication application);
    int updateStatus(LoanApplication application);
    LoanApplication selectById(Long id);
    List<LoanApplication> selectList(@Param("userId") Long userId);

    /** 统计今日新增贷款申请数 */
    int countTodayApplications();

    /** 查询全平台已放款合约的总金额 */
    BigDecimal sumDisbursed();

    /** 按产品类型统计贷款申请笔数（用于产品分布图表） */
    @Select("SELECT COALESCE(p.name, '旧版无产品单') AS name, COUNT(a.id) AS value " +
            "FROM loan_application a " +
            "LEFT JOIN loan_product p ON a.product_id = p.id " +
            "GROUP BY p.name")
    List<Map<String, Object>> countByProduct();

    /** 查询近7日每日放款总金额与申请笔数（用于趋势图表） */
    @Select("SELECT DATE(a.apply_time) AS date, " +
            "COALESCE(SUM(CASE WHEN a.status = 1 THEN a.amount ELSE 0 END), 0) AS dailyDisbursed, " +
            "COUNT(a.id) AS dailyCount " +
            "FROM loan_application a " +
            "WHERE a.apply_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(a.apply_time) " +
            "ORDER BY DATE(a.apply_time) ASC")
    List<Map<String, Object>> selectWeeklyTrend();
}
