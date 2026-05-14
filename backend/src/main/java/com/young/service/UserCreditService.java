package com.young.service;

import com.young.pojo.UserCredit;
import java.math.BigDecimal;

public interface UserCreditService {
    
    /**
     * 客户查询自身可用额度
     */
    UserCredit getMyCredit(Long userId);

    /**
     * 管理员为主体调整（新增/减少）总授信额度
     */
    void adjustTotalCredit(Long adminId, Long targetUserId, BigDecimal newTotal);
    
    /**
     * 管理员风险管控，一键冻结额度使用
     */
    void freezeCredit(Long targetUserId, String reason);
    
    /**
     * 管理员或者系统解除额度冻结
     */
    void unfreezeCreditAuth(Long targetUserId);
    
    /**
     * 【管理端】查询全平台所有用户的授信情况
     */
    java.util.List<UserCredit> getAllCredits();
}
