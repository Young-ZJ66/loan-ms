package com.young.service;

import com.young.pojo.CollectionRecord;
import com.young.pojo.RepaymentPlan;

import java.util.List;

/**
 * 催收管理服务接口
 */
public interface CollectionService {

    /** 查询全部逾期账单 */
    List<RepaymentPlan> getOverduePlans();

    /**
     * 发起催收动作并同步推送站内通知
     * @return 逾期账单关联信息（用于回显）
     */
    void collect(Long planId, String method, String resultDesc, Long adminId);

    /** 查询指定账单的催收记录 */
    List<CollectionRecord> getRecords(Long planId);
}
