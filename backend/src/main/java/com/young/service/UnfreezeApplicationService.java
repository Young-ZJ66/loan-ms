package com.young.service;

import com.young.pojo.UnfreezeApplication;

import java.util.List;

/**
 * 解冻申诉服务接口
 */
public interface UnfreezeApplicationService {

    /** 客户端发起申诉解冻工单 */
    void applyUnfreeze(Long userId, String reason);

    /** 查询我正在审批途中的工单状态 */
    UnfreezeApplication getMyPending(Long userId);

    /** 查询全部历史解冻申诉记录（管理端） */
    List<UnfreezeApplication> listAll();

    /** 后台管理员审批与决断申诉 */
    void audit(Long id, boolean isPass, Long adminId);
}
