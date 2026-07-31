package com.young.service;

import java.util.Map;

/**
 * 管理端数据统计服务接口
 */
public interface AdminStatService {

    /** 获取宏观大盘汇总指标 */
    Map<String, Object> getOverview();

    /** 获取注册用户基数 */
    Integer getUserCount();

    /** 获取待审批红点数量聚合 */
    Map<String, Object> getBadges();

    /** 获取贷款产品分布（按产品类型统计申请笔数） */
    java.util.List<Map<String, Object>> getProductDistribution();

    /** 获取近7日授信与款项流动趋势 */
    java.util.List<Map<String, Object>> getWeeklyTrend();
}
