package com.young.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.young.common.Result;
import com.young.pojo.RepaymentPlan;
import com.young.service.RepaymentPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Api(tags = "还款计划管理")
@RestController
@RequestMapping("/api/repayment")
public class RepaymentPlanController {

    @Autowired
    private RepaymentPlanService repaymentService;

    /**
     * [客户端] 获取我的账单
     */
    @ApiOperation("查询我的还款计划")
    @GetMapping("/my-plans")
    public Result<List<RepaymentPlan>> getMyPlans(@RequestAttribute("userId") Long userId, 
                                                  @RequestParam(required = false) Integer status) {
        List<RepaymentPlan> list = repaymentService.getUserPlans(userId, status);
        return Result.success(list);
    }

    /**
     * [客户端] 支付某一期分期账单 (正常/逾期清欠)
     */
    @ApiOperation("支付指定期还款账单")
    @PostMapping("/pay")
    public Result<?> payInstallment(@RequestAttribute("userId") Long userId, 
                                    @RequestParam Long planId, 
                                    @RequestParam BigDecimal payAmount) {
        repaymentService.payNormalInstallment(userId, planId, payAmount);
        return Result.success("账单支付成功");
    }

    /**
     * [客户端] 提前结清整笔贷款
     */
    @ApiOperation("提前结清整笔贷款")
    @PostMapping("/pay-early/{loanId}")
    public Result<?> payEarlySettlement(@RequestAttribute("userId") Long userId, 
                                        @PathVariable Long loanId) {
        repaymentService.payEarlySettlement(userId, loanId);
        return Result.success("该笔贷款已全部提前结清");
    }
}
