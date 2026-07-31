package com.young.service.impl;

import com.young.common.BusinessException;
import com.young.mapper.UnfreezeApplicationMapper;
import com.young.pojo.UnfreezeApplication;
import com.young.service.UnfreezeApplicationService;
import com.young.service.UserCreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 解冻申诉服务实现
 */
@Service
public class UnfreezeApplicationServiceImpl implements UnfreezeApplicationService {

    @Autowired
    private UnfreezeApplicationMapper applicationMapper;
    @Autowired
    private UserCreditService creditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyUnfreeze(Long userId, String reason) {
        UnfreezeApplication pending = applicationMapper.selectLatestPendingByUserId(userId);
        if (pending != null) {
            throw new BusinessException("您已有正在被审查中的解冻申诉，不可重复提交");
        }
        UnfreezeApplication app = new UnfreezeApplication();
        app.setUserId(userId);
        app.setReason(reason);
        app.setStatus(0);
        applicationMapper.insert(app);
    }

    @Override
    public UnfreezeApplication getMyPending(Long userId) {
        return applicationMapper.selectLatestPendingByUserId(userId);
    }

    @Override
    public List<UnfreezeApplication> listAll() {
        return applicationMapper.selectList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, boolean isPass, Long adminId) {
        UnfreezeApplication app = applicationMapper.selectById(id);
        if (app == null || app.getStatus() != 0) {
            throw new BusinessException("非法审批");
        }
        app.setStatus(isPass ? 1 : 2);
        app.setAdminId(adminId);
        applicationMapper.update(app);

        if (isPass) {
            creditService.unfreezeCreditAuth(app.getUserId());
        }
    }
}
