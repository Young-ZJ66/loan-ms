package com.young.controller;

import com.young.common.Result;
import com.young.pojo.UnfreezeApplication;
import com.young.mapper.UnfreezeApplicationMapper;
import com.young.service.UserCreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 解冻申诉工单控制器
 * 负责接收用户的解冻请求，并提供给管理员进行人工复核与审批
 */
@Api(tags = "解冻申诉审批管理")
@RestController
@RequestMapping("/api/unfreeze")
public class UnfreezeApplicationController {

    @Autowired
    private UnfreezeApplicationMapper applicationMapper;

    @Autowired
    private UserCreditService creditService;

    @ApiOperation("客户端发起申诉解冻工单")
    @PostMapping("/apply")
    public Result<?> applyUnfreeze(@RequestAttribute("userId") Long userId, @RequestParam String reason) {
        UnfreezeApplication pending = applicationMapper.selectLatestPendingByUserId(userId);
        if (pending != null) {
            return Result.error("您已有正在被审查中的解冻申诉，不可重复提交");
        }
        UnfreezeApplication app = new UnfreezeApplication();
        app.setUserId(userId);
        app.setReason(reason);
        app.setStatus(0);
        applicationMapper.insert(app);
        return Result.success("解除冻结的申诉工单已下发风控组工作台！");
    }

    @ApiOperation("查询我正在审批途中的工单状态")
    @GetMapping("/my_pending")
    public Result<UnfreezeApplication> getMyPending(@RequestAttribute("userId") Long userId) {
        return Result.success(applicationMapper.selectLatestPendingByUserId(userId));
    }

    @ApiOperation("查询全部历史解冻申诉记录（管理端）")
    @GetMapping("/all")
    public Result<List<UnfreezeApplication>> listUnfreezeApplications() {
        return Result.success(applicationMapper.selectList());
    }

    @ApiOperation("后台管理员审批与决断申诉")
    @PostMapping("/audit/{id}")
    public Result<?> auditUnfreeze(@RequestAttribute("userId") Long adminId, @PathVariable Long id,
            @RequestParam boolean isPass) {
        UnfreezeApplication app = applicationMapper.selectById(id);
        if (app == null || app.getStatus() != 0) {
            return Result.error("非法审批");
        }
        app.setStatus(isPass ? 1 : 2);
        app.setAdminId(adminId);
        applicationMapper.update(app);

        if (isPass) {
            creditService.unfreezeCreditAuth(app.getUserId());
        }
        return Result.success("审批决议生效");
    }
}
