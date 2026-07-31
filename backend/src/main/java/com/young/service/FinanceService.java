package com.young.service;

import com.young.pojo.RepaymentPlan;
import com.young.pojo.RepaymentRecord;

import java.util.List;

/**
 * 财务分析服务接口
 */
public interface FinanceService {

    /** 查询全平台所有还款计划 */
    List<RepaymentPlan> getAllPlans();

    /** 查询全平台所有历史入账明细 */
    List<RepaymentRecord> getAllRecords();

    /** 手动触发逾期清算任务 */
    void triggerOverdueScan();
}
