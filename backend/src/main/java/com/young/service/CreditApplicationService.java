package com.young.service;

import com.young.pojo.CreditApplication;

import java.math.BigDecimal;
import java.util.List;

public interface CreditApplicationService {

    void apply(Long userId, BigDecimal amount);

    CreditApplication getMyLatestPending(Long userId);

    List<CreditApplication> getPendingList();

    void approve(Long applicationId, BigDecimal approveAmount, Long adminId);

    void reject(Long applicationId, Long adminId);
}
