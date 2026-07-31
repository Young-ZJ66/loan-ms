package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.Result;
import com.young.pojo.UserProfile;
import com.young.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "实名认证档案管理")
@RestController
@RequestMapping("/api/kyc")
public class UserProfileController {
    
    @Autowired
    private UserProfileService profileService;

    @Operation(summary = "提交实名认证材料")
    @PostMapping("/submit")
    public Result<?> submit(@RequestAttribute("userId") Long userId, @RequestBody UserProfile data) {
        data.setUserId(userId);
        profileService.submitKyc(data);
        return Result.success("实名认证材料已提交，请等待人工审核");
    }

    @Operation(summary = "查询我的实名认证信息")
    @GetMapping("/my")
    public Result<UserProfile> getMyKyc(@RequestAttribute("userId") Long userId) {
        return Result.success(profileService.getMyProfile(userId));
    }

    @Operation(summary = "查询待审核档案列表（管理端）")
    @GetMapping("/pending")
    public Result<List<UserProfile>> listPending(@RequestAttribute("role") Integer role) {
        if (role == null || role != 1) {
            return Result.error(403, "权限不足：只有管理员可以访问此接口");
        }
        return Result.success(profileService.getPendingKycList());
    }

    @Operation(summary = "查询全部档案列表")
    @GetMapping("/all")
    public Result<List<UserProfile>> listAll(@RequestAttribute("role") Integer role) {
        if (role == null || role != 1) {
            return Result.error(403, "权限不足：只有管理员可以访问此接口");
        }
        return Result.success(profileService.getAllProfileList());
    }

    @Operation(summary = "审批实名认证档案")
    @PostMapping("/audit/{id}")
    public Result<?> audit(
            @RequestAttribute("userId") Long adminId, 
            @RequestAttribute("role") Integer role, 
            @PathVariable Long id, 
            @RequestParam boolean isPass) {
        if (role == null || role != 1) {
            return Result.error(403, "权限不足：只有管理员可以执行此操作");
        }
        profileService.auditKyc(adminId, id, isPass);
        return Result.success(isPass ? "实名审核已通过" : "实名审核已驳回");
    }
}
