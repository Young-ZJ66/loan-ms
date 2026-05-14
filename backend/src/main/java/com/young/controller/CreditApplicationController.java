package com.young.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.young.common.Result;
import com.young.pojo.CreditApplication;
import com.young.service.CreditApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Api(tags = "授信频度申请管理")
@RestController
@RequestMapping("/api/credit-app")
public class CreditApplicationController {

    @Autowired
    private CreditApplicationService applicationService;

    /**
     * [客户端] 提交额度申请
     */
    @ApiOperation("提交额度申请")
    @PostMapping("/apply")
    public Result<?> apply(@RequestAttribute("userId") Long userId, 
                           @RequestParam BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("1000")) < 0) {
            return Result.error("期望申请额度不能低于1000元");
        }
        applicationService.apply(userId, amount);
        return Result.success("申请提交成功，请耐心等待风险模型与人工协同审批。");
    }

    /**
     * [客户端] 查询自己当前是否有尚未结案的提额申请
     */
    @ApiOperation("查询我的待审申请")
    @GetMapping("/my_pending")
    public Result<CreditApplication> getMyPending(@RequestAttribute("userId") Long userId) {
        return Result.success(applicationService.getMyLatestPending(userId));
    }

    /**
     * [管理端] 获取所有用户的待审批建额/提额请求单
     */
    @ApiOperation("查询待审批申请列表（管理端）")
    @GetMapping("/pending")
    public Result<List<CreditApplication>> getPendingList() {
        return Result.success(applicationService.getPendingList());
    }

    /**
     * [管理端] 审批通过并发放最终授信额度（支持管理员修正预期金额）
     */
    @ApiOperation("审批通过并下发额度")
    @PostMapping("/approve/{id}")
    public Result<?> approve(@PathVariable Long id, 
                             @RequestParam BigDecimal approveAmount, 
                             @RequestAttribute("userId") Long adminId) {
        if (approveAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("下发额度必须大于0");
        }
        applicationService.approve(id, approveAmount, adminId);
        return Result.success("已成功下发授权额度！");
    }

    /**
     * [管理端] 驳回提额申请
     */
    @ApiOperation("驳回提额申请")
    @PostMapping("/reject/{id}")
    public Result<?> reject(@PathVariable Long id, 
                            @RequestAttribute("userId") Long adminId) {
        applicationService.reject(id, adminId);
        return Result.success("提额申请已驳回打回");
    }
}
