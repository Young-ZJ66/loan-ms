package com.young.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.young.common.Result;
import com.young.pojo.LoanApplication;
import com.young.service.LoanApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "贷款申请管理")
@RestController
@RequestMapping("/api/loan")
public class LoanApplicationController {

    @Autowired
    private LoanApplicationService loanService;

    /**
     * [客户端] 提交贷款申请
     */
    @ApiOperation("提交贷款申请")
    @PostMapping("/apply")
    public Result<?> applyLoan(@RequestAttribute("userId") Long userId, @RequestBody LoanApplication application) {
        loanService.applyLoan(userId, application);
        return Result.success("贷款申请已提交，等待后台审核");
    }

    /**
     * [管理端] 获取所有待审批的融资申请
     */
    @ApiOperation("查询待审批贷款列表（管理端）")
    @GetMapping("/pending")
    public Result<?> listPending() {
        return Result.success(loanService.getApplicationList(null).stream()
                .filter(a -> a.getStatus() == 0).toList());
    }

    /**
     * [管理端] 审批通过并放款
     */
    @ApiOperation("审批通过并放款")
    @PostMapping("/approve/{appId}")
    public Result<?> approveAndDisburse(@RequestAttribute("userId") Long adminId, @PathVariable Long appId) {
        loanService.approveAndDisburse(appId);
        return Result.success("放款成功，账单计划已自动生成下发");
    }

    /**
     * [管理端] 驳回贷款
     */
    @ApiOperation("驳回贷款申请")
    @PostMapping("/reject/{appId}")
    public Result<?> rejectLoan(@PathVariable Long appId) {
        loanService.rejectLoan(appId);
        return Result.success("申请已被驳回");
    }

    /**
     * [双视界] 获取贷款列表 (供管理端)
     */
    @ApiOperation("查询贷款申请列表")
    @GetMapping("/list")
    public Result<List<LoanApplication>> getList(@RequestParam(required = false) Long userId) {
        List<LoanApplication> list = loanService.getApplicationList(userId);
        return Result.success(list);
    }

    /**
     * [客户端] 获取我的贷款申请记录
     */
    @ApiOperation("查询我的贷款申请列表")
    @GetMapping("/my")
    public Result<List<LoanApplication>> getMyList(@RequestAttribute("userId") Long userId) {
        List<LoanApplication> list = loanService.getApplicationList(userId);
        return Result.success(list);
    }
}
