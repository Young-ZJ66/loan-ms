package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
    @PostMapping("/adjust")
    public Result<?> adjustCredit(@RequestAttribute("userId") Long adminId,
                                  @RequestAttribute("role") Integer role,
                                  @RequestParam Long targetUserId,
                                  @RequestParam BigDecimal newTotal) {
        if (role == null || role != 1) {
            return Result.error(403, "权限不足：只有管理员可以调整用户授信总额度");
        }
        creditService.adjustTotalCredit(adminId, targetUserId, newTotal);
        return Result.success("额度调整成功");
    }

    /**
     * [管理端] 风控冻结用户
     */
    @Operation(summary = "风控冻结用户账户")
    @PostMapping("/freeze/{targetUserId}")
    public Result<?> freezeCredit(
            @RequestAttribute("role") Integer role,
            @PathVariable Long targetUserId, 
            @RequestParam(required = false) String reason) {
        if (role == null || role != 1) {
            return Result.error(403, "权限不足：只有管理员可以执行冻结账户操作");
        }
        creditService.freezeCredit(targetUserId, reason);
        return Result.success("该用户可用额度已被冻结，系统消息已下发");
    }

    /**
     * [管理端] 解除冻结用户
     */
    @Operation(summary = "解冻用户账户")
    @PostMapping("/unfreeze/{targetUserId}")
    public Result<?> unfreezeCredit(@RequestAttribute("role") Integer role, @PathVariable Long targetUserId) {
        if (role == null || role != 1) {
            return Result.error(403, "权限不足：只有管理员可以执行解冻账户操作");
        }
        creditService.unfreezeCreditAuth(targetUserId);
        return Result.success("账户风控解除！");
    }
    
    /**
     * [管理端] 查询全平台所有用户授信情况
     */
    @Operation(summary = "查询全平台用户授信情况")
    @GetMapping("/all")
    public Result<?> getAllCredits(@RequestAttribute("role") Integer role) {
        if (role == null || role != 1) {
            return Result.error(403, "权限不足：只有管理员可以访问此接口");
        }
        return Result.success(creditService.getAllCredits());
    }
}
