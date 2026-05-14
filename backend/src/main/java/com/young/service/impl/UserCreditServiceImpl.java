package com.young.service.impl;

import com.young.mapper.RepaymentPlanMapper;
import com.young.mapper.SysMessageMapper;
import com.young.mapper.UserCreditMapper;
import com.young.mapper.UnfreezeApplicationMapper;
import com.young.pojo.SysMessage;
import com.young.pojo.UserCredit;
import com.young.service.UserCreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserCreditServiceImpl implements UserCreditService {

    @Autowired
    private UserCreditMapper creditMapper;

    @Autowired
    private RepaymentPlanMapper planMapper;

    @Autowired
    private SysMessageMapper messageMapper;

    @Autowired
    private UnfreezeApplicationMapper unfreezeAppMapper;

    @Override
    public UserCredit getMyCredit(Long userId) {
        UserCredit credit = creditMapper.selectByUserId(userId);
        if (credit != null && credit.getTotalCredit() != null) {
            // 核对未结清本金以计算实际可用额度
            BigDecimal realUsed = planMapper.sumUnpaidPrincipal(userId);
            BigDecimal theoreticalAvailable = credit.getTotalCredit().subtract(realUsed);

            // 若可用额度存在计算误差，则触发账单自愈机制修复额度
            if (credit.getAvailableCredit() == null ||
                    credit.getAvailableCredit().subtract(theoreticalAvailable).abs()
                            .compareTo(new BigDecimal("0.01")) > 0) {

                credit.setUsedCredit(realUsed);
                credit.setAvailableCredit(theoreticalAvailable);
                creditMapper.updateCredit(credit);
            }

            // 判断是否处于风控管制，是的话则隐匿其原本可用额度
            if (credit.getStatus() == 0) {
                credit.setAvailableCredit(BigDecimal.ZERO);
            }
        }
        return credit;
    }

    @Override
    public void adjustTotalCredit(Long adminId, Long targetUserId, BigDecimal newTotal) {
        UserCredit credit = creditMapper.selectByUserId(targetUserId);
        if (credit != null) {
            BigDecimal diff = newTotal.subtract(credit.getTotalCredit());
            credit.setTotalCredit(newTotal);
            // 同步放大/缩小可用额度
            credit.setAvailableCredit(credit.getAvailableCredit().add(diff));
            creditMapper.updateCredit(credit);
        }
    }

    @Override
    @Transactional
    public void freezeCredit(Long targetUserId, String reason) {
        UserCredit current = creditMapper.selectByUserId(targetUserId);
        if (current != null && current.getStatus() == 1) {
            creditMapper.updateStatus(targetUserId, 0);

            // 下发站内信告知冻结状态
            SysMessage sysMsg = new SysMessage();
            sysMsg.setToUserId(targetUserId);
            sysMsg.setTitle("【账户冻结通知】");
            sysMsg.setContent("您的风控信用账户已被临时停止使用。原因：" + (reason != null && !reason.isEmpty() ? reason : "触发系统风控审计规则")
                    + "。若有异议可在首页提交解冻申诉。");
            sysMsg.setIsRead(0);
            messageMapper.insert(sysMsg);
        }
    }

    @Override
    @Transactional
    public void unfreezeCreditAuth(Long targetUserId) {
        UserCredit current = creditMapper.selectByUserId(targetUserId);
        if (current != null && current.getStatus() == 0) {
            creditMapper.updateStatus(targetUserId, 1);

            // 下发解冻状态站内通知
            SysMessage sysMsg = new SysMessage();
            sysMsg.setToUserId(targetUserId);
            sysMsg.setTitle("【账户风险解除通知】");
            sysMsg.setContent("经人工审核确认，您的信用贷款账户已解除冻结，原授信额度恢复使用。");
            sysMsg.setIsRead(0);
            messageMapper.insert(sysMsg);

            // 同步签批并核销该用户目前挂起的解冻业务单
            com.young.pojo.UnfreezeApplication pending = unfreezeAppMapper.selectLatestPendingByUserId(targetUserId);
            if (pending != null) {
                pending.setStatus(1);
                pending.setAdminId(0L);
                unfreezeAppMapper.update(pending);
            }
        }
    }

    @Override
    public java.util.List<UserCredit> getAllCredits() {
        return creditMapper.selectAll();
    }
}
