package com.young.mapper;

import com.young.pojo.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper {
    SysUser selectByUsername(String username);
    
    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser selectById(Long id);
    
    @Update("UPDATE sys_user SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    int insert(SysUser sysUser);

    /** 只统计普通用户（role=0，排除管理员） */
    @Select("SELECT count(id) FROM sys_user WHERE role = 0")
    Integer countUsers();
    
    /** 统计今日新注册用户数 */
    @Select("SELECT count(id) FROM sys_user WHERE role = 0 AND DATE(create_time) = CURDATE()")
    Integer countTodayNew();
}
