package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.SysMessageMapper;
import com.young.mapper.UserCreditMapper;
import com.young.pojo.SysMessage;
import com.young.pojo.UserCredit;
import com.young.service.UserCreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserCreditServiceImpl implements UserCreditService {

    @Autowired
    private UserCreditMapper creditMapper;

    @Autowired
    private RepaymentPlanMapper planMapper;

    @Autowired
    private SysMessageMapper messageMapper;

    @Override
    public UserCredit getMyCredit(Long userId) {
        // 纯记账模型：直接以 user_credit 表数据为准，不在查询方法中执行写库自愈
        // available_credit 字段已由申请/还款流程实时维护，无需重算
        UserCredit credit = creditMapper.selectByUserId(userId);
        if (credit != null && credit.getStatus() != null && credit.getStatus() == 0) {
            // 账户被风控冻结时，前端展示可用额度为 0
            credit.setAvailableCredit(BigDecimal.ZERO);
        }
        return credit;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustTotalCredit(Long adminId, Long targetUserId, BigDecimal newTotal) {
        if (newTotal == null || newTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("调整后的授信额度必须大于0");
        }
        UserCredit credit = creditMapper.selectByUserId(targetUserId);
        if (credit == null) {
            throw new BusinessException("目标用户尚未建立授信额度");
        }
        // 降额时不允许低于当前已用额度，避免破坏 total = available + used 不变量
        if (newTotal.compareTo(credit.getUsedCredit()) < 0) {
            throw new BusinessException("调整后的总额度不得低于当前已用额度 " + credit.getUsedCredit() + " 元");
        }
        BigDecimal diff = newTotal.subtract(credit.getTotalCredit());
        credit.setTotalCredit(newTotal);
        // 同步调整可用额度，保持 total = available + used
        BigDecimal newAvailable = credit.getAvailableCredit().add(diff);
        credit.setAvailableCredit(newAvailable);
        creditMapper.updateCredit(credit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeCredit(Long targetUserId, String reason, Long adminId) {
        UserCredit current = creditMapper.selectByUserId(targetUserId);
        if (current == null) {
            throw new BusinessException("目标用户不存在授信记录");
        }
        if (current.getStatus() == 0) {
            throw new BusinessException("该账户当前已处于冻结状态");
        }
        creditMapper.updateStatus(targetUserId, 0);

        SysMessage sysMsg = new SysMessage();
        sysMsg.setToUserId(targetUserId);
        sysMsg.setTitle("【账户冻结通知】");
        sysMsg.setContent("您的风控信用账户已被临时停止使用。原因：" + (reason != null && !reason.isEmpty() ? reason : "触发系统风控审计规则")
                + "。若有异议可在首页提交解冻申诉。");
        sysMsg.setIsRead(0);
        messageMapper.insert(sysMsg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeCreditAuth(Long targetUserId, Long adminId) {
        UserCredit current = creditMapper.selectByUserId(targetUserId);
        if (current == null) {
            throw new BusinessException("目标用户不存在授信记录");
        }
        if (current.getStatus() != 0) {
            throw new BusinessException("该账户当前未被冻结，无需解除");
        }
        creditMapper.updateStatus(targetUserId, 1);

        SysMessage sysMsg = new SysMessage();
        sysMsg.setToUserId(targetUserId);
        sysMsg.setTitle("【账户风险解除通知】");
        sysMsg.setContent("经人工审核确认，您的信用贷款账户已解除冻结，原授信额度恢复使用。");
        sysMsg.setIsRead(0);
        messageMapper.insert(sysMsg);
    }

    @Override
    public List<UserCredit> getAllCredits() {
        return creditMapper.selectAll();
    }
}
