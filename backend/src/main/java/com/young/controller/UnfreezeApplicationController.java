package com.young.controller;

import com.young.common.RequireRole;
import com.young.common.Result;
import com.young.pojo.UnfreezeApplication;
import com.young.service.UnfreezeApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 解冻申诉工单控制器
 * 负责接收用户的解冻请求，并提供给管理员进行人工复核与审批
 */
@Tag(name = "解冻申诉审批管理")
@RestController
@RequestMapping("/api/unfreeze")
public class UnfreezeApplicationController {

    @Autowired
    private UnfreezeApplicationService unfreezeService;

    @Operation(summary = "客户端发起申诉解冻工单")
    @PostMapping("/apply")
    public Result<?> applyUnfreeze(@RequestAttribute("userId") Long userId, @RequestParam String reason) {
        unfreezeService.applyUnfreeze(userId, reason);
        return Result.success("解除冻结的申诉工单已下发风控组工作台！");
    }

    @Operation(summary = "查询我正在审批途中的工单状态")
    @GetMapping("/my_pending")
    public Result<UnfreezeApplication> getMyPending(@RequestAttribute("userId") Long userId) {
        return Result.success(unfreezeService.getMyPending(userId));
    }

    @Operation(summary = "查询全部历史解冻申诉记录（管理端）")
    @RequireRole
    @GetMapping("/all")
    public Result<List<UnfreezeApplication>> listUnfreezeApplications() {
        return Result.success(unfreezeService.listAll());
    }

    @Operation(summary = "后台管理员审批与决断申诉")
    @RequireRole
    @PostMapping("/audit/{id}")
    public Result<?> auditUnfreeze(@RequestAttribute("userId") Long adminId,
                                   @PathVariable Long id,
                                   @RequestParam boolean isPass) {
        unfreezeService.audit(id, isPass, adminId);
        return Result.success("审批决议生效");
    }
}
