package com.young.controller;

import com.young.common.Result;
import com.young.mapper.LoanApplicationMapper;
import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.SysUserMapper;
import com.young.mapper.UnfreezeApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 仪表盘与数据中心控制器
 * 提供全局经营数据的统计能力及各项管理指标计算接口
 */
@Api(tags = "数据监控看板管理")
@RestController
@RequestMapping("/api/admin/stat")
public class AdminStatController {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private LoanApplicationMapper loanMapper;
    @Autowired
    private RepaymentPlanMapper planMapper;

    /**
     * [管理端] 获取监控大盘汇总数据（一次性返回所有指标，减少多次请求）
     */
    @ApiOperation("获取宏观大盘汇总指标")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> data = new HashMap<>();
        // 1. 总客户量（排除管理员账号）
        data.put("totalUsers", sysUserMapper.countUsers());
        // 2. 今日新增申请数
        data.put("todayApplications", loanMapper.countTodayApplications());
        // 3. 全平台已放款总金额
        BigDecimal disbursed = loanMapper.sumDisbursed();
        data.put("totalDisbursed", disbursed != null ? disbursed : BigDecimal.ZERO);
        // 4. 全平台逾期坏账金额
        BigDecimal overdue = planMapper.sumOverdueAmount();
        data.put("totalOverdue", overdue != null ? overdue : BigDecimal.ZERO);
        return Result.success(data);
    }

    /**
     * 获取单一的用户总数统计
     * 为了兼容历史客户端而保留的基础接口
     */
    @ApiOperation("获取注册用户基数")
    @GetMapping("/users/count")
    public Result<Integer> getUserCount() {
        return Result.success(sysUserMapper.countUsers());
    }

    @Autowired
    private com.young.mapper.UserProfileMapper userProfileMapper;
    @Autowired
    private com.young.mapper.CreditApplicationMapper creditApplicationMapper;
    @Autowired
    private UnfreezeApplicationMapper unfreezeApplicationMapper;

    /**
     * [管理端] 获取左侧菜单栏各种待办小红点数字
     */
    @ApiOperation("获取待审批红点数量聚合")
    @GetMapping("/badges")
    public Result<Map<String, Object>> getBadges() {
        Map<String, Object> badges = new HashMap<>();

        // 待审核的实名资料 (客户管理)
        badges.put("kyc", userProfileMapper.selectPendingList().size());

        // 待审批的贷款申请
        int loanCount = (int) loanMapper.selectList(null).stream().filter(l -> l.getStatus() == 0).count();
        // 待审批的提额申请
        int creditCount = creditApplicationMapper.selectPendingList().size();
        // 待审批的解冻申请
        int unfreezeCount = unfreezeApplicationMapper.countPending();

        badges.put("loan", loanCount);
        badges.put("credit", creditCount);
        badges.put("unfreeze", unfreezeCount);

        // 处于逾期状态的账单 (财务中心)
        badges.put("overdue", planMapper.selectOverduePlans(new java.util.Date()).size());

        return Result.success(badges);
    }
}
