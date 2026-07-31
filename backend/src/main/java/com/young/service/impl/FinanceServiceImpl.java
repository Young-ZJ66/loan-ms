package com.young.service.impl;

import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.RepaymentRecordMapper;
import com.young.pojo.RepaymentPlan;
import com.young.pojo.RepaymentRecord;
import com.young.service.FinanceService;
import com.young.task.OverdueScanTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 财务分析服务实现
 */
@Service
public class FinanceServiceImpl implements FinanceService {

    @Autowired
    private RepaymentPlanMapper planMapper;
    @Autowired
    private RepaymentRecordMapper recordMapper;
    @Autowired
    private OverdueScanTask overdueScanTask;

    @Override
    public List<RepaymentPlan> getAllPlans() {
        return planMapper.selectAll();
    }

    @Override
    public List<RepaymentRecord> getAllRecords() {
        return recordMapper.selectAll();
    }

    @Override
    public void triggerOverdueScan() {
        overdueScanTask.triggerManually();
    }
}
