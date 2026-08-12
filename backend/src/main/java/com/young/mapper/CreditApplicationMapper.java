package com.young.mapper;

import com.young.pojo.CreditApplication;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CreditApplicationMapper {
    int insert(CreditApplication application);
    int update(CreditApplication application);
    /** CAS 更新：仅当当前状态为待审批(0)时才更新，返回受影响行数用于并发抢占 */
    int updateStatusIfPending(CreditApplication application);
    CreditApplication selectById(Long id);
    CreditApplication selectLatestPendingByUserId(Long userId);
    
    /** 查询待审核的额度申请 */
    List<CreditApplication> selectPendingList();
}
