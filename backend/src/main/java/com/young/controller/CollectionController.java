package com.young.controller;

import com.young.common.BusinessException;
import com.young.common.RequireRole;
import com.young.common.Result;
import com.young.pojo.CollectionRecord;
import com.young.pojo.RepaymentPlan;
import com.young.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员催收台：发起催收记录 + 同步向贷款人推送站内催收提醒
 */
@Tag(name = "催收管理")
@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    /**
     * 获取当前所有逾期账单（用于管理员催收台展示）
     */
    @Operation(summary = "查询全部逾期账单")
    @RequireRole
    @GetMapping("/overdue-plans")
    public Result<List<RepaymentPlan>> getOverduePlans() {
        return Result.success(collectionService.getOverduePlans());
    }

    /**
     * 管理员发起一次催收动作，并同步向贷款人推送站内催收通知
     * 请求体: { planId, method, result }
     */
    @Operation(summary = "发起催收动作")
    @RequireRole
    @PostMapping("/action")
    public Result<String> collect(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");

        // 参数安全提取，防止 NPE
        Object planIdObj = body.get("planId");
        Object methodObj = body.get("method");
        Object resultObj = body.get("result");
        if (planIdObj == null || methodObj == null || resultObj == null) {
            throw new BusinessException("参数不完整：planId、method、result 均不能为空");
        }

        Long planId;
        try {
            planId = Long.parseLong(planIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException("planId 格式不合法");
        }

        collectionService.collect(planId, methodObj.toString(), resultObj.toString(), adminId);
        return Result.success("催收记录已记录，站内提醒已发送给贷款人");
    }

    /**
     * 查询指定逾期账单的历次催收记录
     */
    @Operation(summary = "查询指定账单的催收记录")
    @RequireRole
    @GetMapping("/records/{planId}")
    public Result<List<CollectionRecord>> getRecords(@PathVariable Long planId) {
        return Result.success(collectionService.getRecords(planId));
    }
}
