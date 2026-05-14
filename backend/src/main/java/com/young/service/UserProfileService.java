package com.young.service;

import com.young.pojo.UserProfile;
import java.util.List;

public interface UserProfileService {
    /** 客户提交实名资料 */
    void submitKyc(UserProfile profile);
    
    /** 查询我的 KYC 状态 */
    UserProfile getMyProfile(Long userId);

    /** 管理端获取待审批列表 */
    List<UserProfile> getPendingKycList();

    /** 管理端获取所有客户档案 */
    List<UserProfile> getAllProfileList();

    /** 管理端进行审批 */
    void auditKyc(Long adminId, Long profileId, boolean isPass, String reason);
}
