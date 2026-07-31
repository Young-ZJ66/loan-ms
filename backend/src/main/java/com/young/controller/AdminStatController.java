package com.young.controller;

import com.young.common.RequireRole;
import com.young.common.Result;
import com.young.service.AdminStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘与数据中心控制器
 * 提供全局经营数据的统计能力及各项管理指标计算接口
 */
@Tag(name = "数据监控看板管理")
@RestController
@RequestMapping("/api/admin/stat")
public class AdminStatController {

    @Autowired
    private AdminStatService adminStatService;

    /**
     * [管理端] 获取监控大盘汇总数据（一次性返回所有指标，减少多次请求）
     */
    @Operation(summary = "获取宏观大盘汇总指标")
    @RequireRole
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(adminStatService.getOverview());
    }

    /**
     * 获取单一的用户总数统计
     * 为了兼容历史客户端而保留的基础接口
     */
    @Operation(summary = "获取注册用户基数")
    @RequireRole
    @GetMapping("/users/count")
    public Result<Integer> getUserCount() {
        return Result.success(adminStatService.getUserCount());
    }

    /**
     * [管理端] 获取左侧菜单栏各种待办小红点数字
     */
    @Operation(summary = "获取待审批红点数量聚合")
    @RequireRole
    @GetMapping("/badges")
    public Result<Map<String, Object>> getBadges() {
        return Result.success(adminStatService.getBadges());
    }

    /**
     * [管理端] 获取贷款产品分布（按产品类型统计申请笔数）
     */
    @Operation(summary = "获取贷款产品分布")
    @RequireRole
    @GetMapping("/product-distribution")
    public Result<List<Map<String, Object>>> getProductDistribution() {
        return Result.success(adminStatService.getProductDistribution());
    }

    /**
     * [管理端] 获取近7日授信与款项流动趋势
     */
    @Operation(summary = "获取近7日趋势数据")
    @RequireRole
    @GetMapping("/weekly-trend")
    public Result<List<Map<String, Object>>> getWeeklyTrend() {
        return Result.success(adminStatService.getWeeklyTrend());
    }
}
