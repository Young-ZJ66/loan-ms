package com.young.mapper;

import com.young.pojo.LoanApplication;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

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
}
