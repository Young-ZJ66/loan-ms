package com.young.mapper;

import com.young.pojo.UnfreezeApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UnfreezeApplicationMapper {
    int insert(UnfreezeApplication application);
    int update(UnfreezeApplication application);
    UnfreezeApplication selectById(Long id);
    UnfreezeApplication selectLatestPendingByUserId(Long userId);
    
    /** 查询所有解冻申请 */
    List<UnfreezeApplication> selectList();
    
    /** 统计待审批数量 */
    @Select("SELECT COUNT(*) FROM unfreeze_application WHERE status = 0")
    int countPending();
}
