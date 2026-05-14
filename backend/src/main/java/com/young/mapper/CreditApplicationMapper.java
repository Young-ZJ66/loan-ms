package com.young.mapper;

import com.young.pojo.CreditApplication;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CreditApplicationMapper {
    int insert(CreditApplication application);
    int update(CreditApplication application);
    CreditApplication selectById(Long id);
    CreditApplication selectLatestPendingByUserId(Long userId);
    
    // 查询所有待审核的额度申请（联表展示客户实名信息）
    List<CreditApplication> selectPendingList();
}
