package com.young.mapper;

import com.young.pojo.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserProfileMapper {
    int insert(UserProfile profile);
    int update(UserProfile profile);
    UserProfile selectByUserId(Long userId);
    UserProfile selectById(Long id);
    
    // 查询待审核的 KYC 申请
    List<UserProfile> selectPendingList();

    // 查询所有系统的客户实名档案
    List<UserProfile> selectAllList();
}
