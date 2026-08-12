package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.RequireRole;
import com.young.common.Result;
import com.young.pojo.UserProfile;
import com.young.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @RequireRole
    @GetMapping("/pending")
    public Result<List<UserProfile>> listPending() {
        return Result.success(profileService.getPendingKycList());
    }

    @Operation(summary = "查询全部档案列表")
    @RequireRole
    @GetMapping("/all")
    public Result<List<UserProfile>> listAll() {
        return Result.success(profileService.getAllProfileList());
    }

    @Operation(summary = "审批实名认证档案")
    @RequireRole
    @PostMapping("/audit/{id}")
    public Result<?> audit(@PathVariable Long id,
                           @RequestParam boolean isPass,
                           @RequestAttribute("userId") Long adminId) {
        profileService.auditKyc(adminId, id, isPass);
        return Result.success(isPass ? "实名审核已通过" : "实名审核已驳回");
    }
}
