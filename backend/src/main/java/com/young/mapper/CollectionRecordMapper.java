package com.young.mapper;

import com.young.pojo.CollectionRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CollectionRecordMapper {
    /** 插入一条催收动作记录 */
    int insert(CollectionRecord record);
    
    /** 按期供计划ID查询该账单的历次催收记录 */
    List<CollectionRecord> selectByPlanId(Long planId);
    
    /** 查询全平台所有催收记录（管理端概览） */
    List<CollectionRecord> selectAll();
}
