package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.RequireRole;
import com.young.common.Result;
import com.young.pojo.UserCredit;
import com.young.service.UserCreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户授信管理")
@RestController
@RequestMapping("/api/credit")
public class UserCreditController {

    @Autowired
    private UserCreditService creditService;

    /**
     * [客户端] 获取我的额度详情
     */
    @Operation(summary = "查询我的额度详情")
    @GetMapping("/my")
    public Result<UserCredit> getMyCredit(@RequestAttribute("userId") Long userId) {
        return Result.success(creditService.getMyCredit(userId));
    }

    /**
     * [管理端] 调整用户的总授信额度
     */
    @Operation(summary = "调整用户授信总额度")
    @RequireRole
    @PostMapping("/adjust")
    public Result<?> adjustCredit(@RequestAttribute("userId") Long adminId,
                                  @RequestParam Long targetUserId,
                                  @RequestParam BigDecimal newTotal) {
        creditService.adjustTotalCredit(adminId, targetUserId, newTotal);
        return Result.success("额度调整成功");
    }

    /**
     * [管理端] 风控冻结用户
     */
    @Operation(summary = "风控冻结用户账户")
    @RequireRole
    @PostMapping("/freeze/{targetUserId}")
    public Result<?> freezeCredit(@PathVariable Long targetUserId,
                                  @RequestParam(required = false) String reason,
                                  @RequestAttribute("userId") Long adminId) {
        creditService.freezeCredit(targetUserId, reason, adminId);
        return Result.success("该用户可用额度已被冻结，系统消息已下发");
    }

    /**
     * [管理端] 解除冻结用户
     */
    @Operation(summary = "解冻用户账户")
    @RequireRole
    @PostMapping("/unfreeze/{targetUserId}")
    public Result<?> unfreezeCredit(@PathVariable Long targetUserId,
                                    @RequestAttribute("userId") Long adminId) {
        creditService.unfreezeCreditAuth(targetUserId, adminId);
        return Result.success("账户风控解除！");
    }
    
    /**
     * [管理端] 查询全平台所有用户授信情况
     */
    @Operation(summary = "查询全平台用户授信情况")
    @RequireRole
    @GetMapping("/all")
    public Result<?> getAllCredits() {
        return Result.success(creditService.getAllCredits());
    }
}
