package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.RequireRole;
import com.young.common.Result;
import com.young.pojo.LoanApplication;
import com.young.service.LoanApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "贷款申请管理")
@RestController
@RequestMapping("/api/loan")
public class LoanApplicationController {

    @Autowired
    private LoanApplicationService loanService;

    /**
     * [客户端] 提交贷款申请
     */
    @Operation(summary = "提交贷款申请")
    @PostMapping("/apply")
    public Result<?> applyLoan(@RequestAttribute("userId") Long userId, @RequestBody LoanApplication application) {
        loanService.applyLoan(userId, application);
        return Result.success("贷款申请已提交，等待后台审核");
    }

    /**
     * [管理端] 获取所有待审批的融资申请
     */
    @Operation(summary = "查询待审批贷款列表（管理端）")
    @RequireRole
    @GetMapping("/pending")
    public Result<?> listPending() {
        return Result.success(loanService.getPendingList());
    }

    /**
     * [管理端] 审批通过并放款
     */
    @Operation(summary = "审批通过并放款")
    @RequireRole
    @PostMapping("/approve/{appId}")
    public Result<?> approveAndDisburse(@PathVariable Long appId) {
        loanService.approveAndDisburse(appId);
        return Result.success("放款成功，账单计划已自动生成下发");
    }

    /**
     * [管理端] 驳回贷款
     */
    @Operation(summary = "驳回贷款申请")
    @RequireRole
    @PostMapping("/reject/{appId}")
    public Result<?> rejectLoan(@PathVariable Long appId) {
        loanService.rejectLoan(appId);
        return Result.success("申请已被驳回");
    }

    /**
     * [双视界] 获取贷款列表 (供管理端)
     */
    @Operation(summary = "查询贷款申请列表")
    @GetMapping("/list")
    public Result<List<LoanApplication>> getList(
            @RequestAttribute("userId") Long loggedUserId,
            @RequestAttribute("role") Integer role,
            @RequestParam(required = false) Long userId) {
        if ((role == null || role != 1) && (userId == null || !userId.equals(loggedUserId))) {
            return Result.error(403, "越权访问：您无权查询其他用户的贷款申请列表");
        }
        List<LoanApplication> list = loanService.getApplicationList(userId);
        return Result.success(list);
    }

    /**
     * [客户端] 获取我的贷款申请记录
     */
    @Operation(summary = "查询我的贷款申请列表")
    @GetMapping("/my")
    public Result<List<LoanApplication>> getMyList(@RequestAttribute("userId") Long userId) {
        List<LoanApplication> list = loanService.getApplicationList(userId);
        return Result.success(list);
    }
}
