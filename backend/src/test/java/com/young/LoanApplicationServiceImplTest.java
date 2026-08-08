package com.young;

import com.young.common.BusinessException;
import com.young.mapper.LoanApplicationMapper;
import com.young.mapper.LoanProductMapper;
import com.young.mapper.UserCreditMapper;
import com.young.mapper.UserProfileMapper;
import com.young.pojo.LoanApplication;
import com.young.pojo.LoanProduct;
import com.young.pojo.UserProfile;
import com.young.service.impl.LoanApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceImplTest {

    @Mock
    private LoanApplicationMapper applicationMapper;
    @Mock
    private UserCreditMapper creditMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private LoanProductMapper productMapper;

    @InjectMocks
    private LoanApplicationServiceImpl service;

    private UserProfile approvedProfile() {
        UserProfile profile = new UserProfile();
        profile.setStatus(1);
        return profile;
    }

    @Test
    void applyLoan_rejectsMissingProduct() {
        when(userProfileMapper.selectByUserId(1L)).thenReturn(approvedProfile());

        LoanApplication app = new LoanApplication();
        app.setAmount(new BigDecimal("1000"));
        app.setTermMonths(3);
        app.setAnnualRate(BigDecimal.ZERO);

        assertThrows(BusinessException.class, () -> service.applyLoan(1L, app));
        verify(creditMapper, never()).freezeAmount(any(), any());
        verify(applicationMapper, never()).insert(any());
    }

    @Test
    void applyLoan_rejectsUnderTermedAmountForProduct() {
        when(userProfileMapper.selectByUserId(1L)).thenReturn(approvedProfile());

        LoanProduct product = new LoanProduct();
        product.setId(10L);
        product.setAnnualRate(new BigDecimal("0.036"));
        product.setMinAmount(new BigDecimal("100"));
        product.setMaxAmount(new BigDecimal("50000"));
        product.setMinTerm(3);
        product.setMaxTerm(12);
        product.setStatus(1);
        when(productMapper.selectById(10L)).thenReturn(product);

        LoanApplication app = new LoanApplication();
        app.setProductId(10L);
        app.setAmount(new BigDecimal("99")); // 低于产品最低额度
        app.setTermMonths(3);

        assertThrows(BusinessException.class, () -> service.applyLoan(1L, app));
    }

    @Test
    void applyLoan_takesRateFromProduct_ignoresClientValue() {
        when(userProfileMapper.selectByUserId(1L)).thenReturn(approvedProfile());
        when(creditMapper.freezeAmount(1L, new BigDecimal("1000"))).thenReturn(1);

        LoanProduct product = new LoanProduct();
        product.setId(10L);
        product.setAnnualRate(new BigDecimal("0.036"));
        product.setMinAmount(new BigDecimal("100"));
        product.setMaxAmount(new BigDecimal("50000"));
        product.setMinTerm(3);
        product.setMaxTerm(12);
        product.setStatus(1);
        when(productMapper.selectById(10L)).thenReturn(product);

        LoanApplication app = new LoanApplication();
        app.setProductId(10L);
        app.setAmount(new BigDecimal("1000"));
        app.setTermMonths(3);
        app.setAnnualRate(BigDecimal.ZERO); // 客户端尝试注入零利率，必须被忽略

        service.applyLoan(1L, app);

        assertEquals(0, app.getAnnualRate().compareTo(new BigDecimal("0.036")));
        assertEquals(1L, app.getUserId());
        assertEquals(0, app.getStatus());
        verify(applicationMapper).insert(app);
    }
}
