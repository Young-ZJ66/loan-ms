package com.young.mapper;

import com.young.pojo.UserCredit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserCreditMapper {
    int insert(UserCredit credit);
    UserCredit selectByUserId(Long userId);
    int updateCredit(UserCredit credit);
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);
    int freezeAmount(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);
    int unfreezeAmount(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);
    
    /** 【管理端】查询全平台所有用户的授信记录 */
    @Select("SELECT * FROM user_credit ORDER BY user_id ASC")
    java.util.List<UserCredit> selectAll();
}
