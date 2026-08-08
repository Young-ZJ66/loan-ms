package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.CollectionRecordMapper;
import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.SysMessageMapper;
import com.young.pojo.CollectionRecord;
import com.young.pojo.RepaymentPlan;
import com.young.pojo.SysMessage;
import com.young.service.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 催收管理服务实现
 */
@Service
public class CollectionServiceImpl implements CollectionService {

    @Autowired
    private CollectionRecordMapper collectionMapper;
    @Autowired
    private RepaymentPlanMapper planMapper;
    @Autowired
    private SysMessageMapper messageMapper;

    @Override
    public List<RepaymentPlan> getOverduePlans() {
        // status=2 的账单即逾期中
        return planMapper.selectOverdueAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collect(Long planId, String method, String resultDesc, Long adminId) {
        if (planId == null) {
            throw new BusinessException("催收账单ID不能为空");
        }
        if (method == null || method.isBlank()) {
            throw new BusinessException("催收方式不能为空");
        }
        if (resultDesc == null || resultDesc.isBlank()) {
            throw new BusinessException("催收结果描述不能为空");
        }

        RepaymentPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("找不到该还款计划，请刷新后重试");
        }

        CollectionRecord record = new CollectionRecord();
        record.setPlanId(planId);
        record.setLoanId(plan.getLoanId());
        record.setUserId(plan.getUserId());
        record.setAdminId(adminId);
        record.setMethod(method);
        record.setResult(resultDesc);
        collectionMapper.insert(record);

        SysMessage msg = new SysMessage();
        msg.setToUserId(plan.getUserId());
        msg.setTitle("逾期催收通知");
        msg.setContent(String.format(
                "您的第 %d 期还款账单已逾期，当前应还总额 %.2f 元（含罚息）。" +
                        "请尽快联系平台处理，催收方式：%s。备注：%s",
                plan.getTermIndex(), plan.getTotalAmount(), method, resultDesc));
        messageMapper.insert(msg);
    }

    @Override
    public List<CollectionRecord> getRecords(Long planId) {
        return collectionMapper.selectByPlanId(planId);
    }
}
