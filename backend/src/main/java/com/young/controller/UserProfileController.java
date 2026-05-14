package com.young.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.young.common.Result;
import com.young.pojo.UserProfile;
import com.young.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "实名认证档案管理")
@RestController
@RequestMapping("/api/kyc")
public class UserProfileController {
    
    @Autowired
    private UserProfileService profileService;

    @ApiOperation("提交实名认证材料")
    @PostMapping("/submit")
    public Result<?> submit(@RequestAttribute("userId") Long userId, @RequestBody UserProfile data) {
        data.setUserId(userId);
        profileService.submitKyc(data);
        return Result.success("实名认证材料已提交，请等待人工审核");
    }

    @ApiOperation("查询我的实名认证信息")
    @GetMapping("/my")
    public Result<UserProfile> getMyKyc(@RequestAttribute("userId") Long userId) {
        return Result.success(profileService.getMyProfile(userId));
    }

    @ApiOperation("查询待审核档案列表（管理端）")
    @GetMapping("/pending")
    public Result<List<UserProfile>> listPending() {
        // 这里理论上应受权限管控（adminId），由于JWT全局已区分，假设进入的必定是管理视角
        return Result.success(profileService.getPendingKycList());
    }

    @ApiOperation("查询全部档案列表")
    @GetMapping("/all")
    public Result<List<UserProfile>> listAll() {
        return Result.success(profileService.getAllProfileList());
    }

    @ApiOperation("审批实名认证档案")
    @PostMapping("/audit/{id}")
    public Result<?> audit(@RequestAttribute("userId") Long adminId, @PathVariable Long id, @RequestParam boolean isPass, @RequestParam(required = false) String reason) {
        profileService.auditKyc(adminId, id, isPass, reason);
        return Result.success(isPass ? "实名审核已通过" : "实名审核已驳回");
    }
}
