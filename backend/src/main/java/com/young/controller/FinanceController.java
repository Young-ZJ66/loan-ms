package com.young.controller;

import com.young.common.Result;
import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.RepaymentRecordMapper;
import com.young.pojo.RepaymentPlan;
import com.young.pojo.RepaymentRecord;
import com.young.task.OverdueScanTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.List;

@Api(tags = "财务分析管理")
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private RepaymentPlanMapper planMapper;
    @Autowired
    private RepaymentRecordMapper recordMapper;
    @Autowired
    private OverdueScanTask overdueScanTask;

    /**
     * [财务中心] 查询全平台所有还款计划（含客户姓名）
     */
    @ApiOperation("查询全平台还款计划")
    @GetMapping("/plans")
    public Result<List<RepaymentPlan>> getAllPlans() {
        return Result.success(planMapper.selectAll());
    }

    /**
     * [财务中心] 查询全平台所有历史入账明细
     */
    @ApiOperation("查询全平台入账明细")
    @GetMapping("/records")
    public Result<List<RepaymentRecord>> getAllRecords() {
        return Result.success(recordMapper.selectAll());
    }

    /**
     * 手动触发逾期清算任务
     * （专供具有权限的运营人员应对突发及错账情况的应急干预）
     */
    @ApiOperation("手动触发逾期清算任务")
    @GetMapping("/trigger-overdue")
    public Result<String> triggerOverdue() {
        overdueScanTask.triggerManually();
        return Result.success("逾期清算任务已手动触发，请查看控制台日志");
    }
}
