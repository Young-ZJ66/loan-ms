package com.young.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.young.common.Result;
import com.young.mapper.CollectionRecordMapper;
import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.SysMessageMapper;
import com.young.pojo.CollectionRecord;
import com.young.pojo.RepaymentPlan;
import com.young.pojo.SysMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 管理员催收台：发起催收记录 + 同步向贷款人推送站内催收提醒
 */
@Api(tags = "催收管理")
@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    @Autowired
    private CollectionRecordMapper collectionMapper;
    @Autowired
    private RepaymentPlanMapper planMapper;
    @Autowired
    private SysMessageMapper messageMapper;

    /**
     * 获取当前所有逾期账单（用于管理员催收台展示）
     */
    @ApiOperation("查询全部逾期账单")
    @GetMapping("/overdue-plans")
    public Result<List<RepaymentPlan>> getOverduePlans() {
        // status=2 的账单即逾期中
        List<RepaymentPlan> all = planMapper.selectAll();
        List<RepaymentPlan> overdue = all.stream()
                .filter(p -> p.getStatus() == 2)
                .toList();
        return Result.success(overdue);
    }

    /**
     * 管理员发起一次催收动作，并同步向贷款人推送站内催收通知
     * 请求体: { planId, method, result }
     */
    @ApiOperation("发起催收动作")
    @PostMapping("/action")
    public Result<String> collect(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");

        Long planId = Long.parseLong(body.get("planId").toString());
        String method = body.get("method").toString();
        String resultDesc = body.get("result").toString();

        // 查找逾期账单详情
        RepaymentPlan plan = planMapper.selectAll().stream()
                .filter(p -> p.getId().equals(planId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("找不到该还款计划，请刷新后重试"));

        // 写入催收动作记录
        CollectionRecord record = new CollectionRecord();
        record.setPlanId(planId);
        record.setLoanId(plan.getLoanId());
        record.setUserId(plan.getUserId());
        record.setAdminId(adminId);
        record.setMethod(method);
        record.setResult(resultDesc);
        collectionMapper.insert(record);

        // 同步向贷款人推送站内催收提醒
        SysMessage msg = new SysMessage();
        msg.setToUserId(plan.getUserId());
        msg.setTitle("🔔 逾期催收通知");
        msg.setContent(String.format(
                "您的第 %d 期还款账单已逾期，当前应还总额 %.2f 元（含罚息）。" +
                        "请尽快联系平台处理，催收方式：%s。备注：%s",
                plan.getTermIndex(), plan.getTotalAmount(), method, resultDesc));
        messageMapper.insert(msg);

        return Result.success("催收记录已记录，站内提醒已发送给贷款人");
    }

    /**
     * 查询指定逾期账单的历次催收记录
     */
    @ApiOperation("查询指定账单的催收记录")
    @GetMapping("/records/{planId}")
    public Result<List<CollectionRecord>> getRecords(@PathVariable Long planId) {
        return Result.success(collectionMapper.selectByPlanId(planId));
    }
}
