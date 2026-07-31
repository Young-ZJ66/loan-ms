package com.young.service.impl;

import com.young.mapper.*;
import com.young.service.AdminStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据统计服务实现
 */
@Service
public class AdminStatServiceImpl implements AdminStatService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private LoanApplicationMapper loanMapper;
    @Autowired
    private RepaymentPlanMapper planMapper;
    @Autowired
    private UserProfileMapper userProfileMapper;
    @Autowired
    private CreditApplicationMapper creditApplicationMapper;
    @Autowired
    private UnfreezeApplicationMapper unfreezeApplicationMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", sysUserMapper.countUsers());
        data.put("todayApplications", loanMapper.countTodayApplications());
        BigDecimal disbursed = loanMapper.sumDisbursed();
        data.put("totalDisbursed", disbursed != null ? disbursed : BigDecimal.ZERO);
        BigDecimal overdue = planMapper.sumOverdueAmount();
        data.put("totalOverdue", overdue != null ? overdue : BigDecimal.ZERO);
        return data;
    }

    @Override
    public Integer getUserCount() {
        return sysUserMapper.countUsers();
    }

    @Override
    public Map<String, Object> getBadges() {
        Map<String, Object> badges = new HashMap<>();

        badges.put("kyc", userProfileMapper.selectPendingList().size());

        int loanCount = (int) loanMapper.selectList(null).stream().filter(l -> l.getStatus() == 0).count();
        int creditCount = creditApplicationMapper.selectPendingList().size();
        int unfreezeCount = unfreezeApplicationMapper.countPending();

        badges.put("loan", loanCount);
        badges.put("credit", creditCount);
        badges.put("unfreeze", unfreezeCount);

        badges.put("overdue", planMapper.selectOverduePlans(new java.util.Date()).size());

        return badges;
    }

    @Override
    public List<Map<String, Object>> getProductDistribution() {
        return loanMapper.countByProduct();
    }

    @Override
    public List<Map<String, Object>> getWeeklyTrend() {
        return loanMapper.selectWeeklyTrend();
    }
}
