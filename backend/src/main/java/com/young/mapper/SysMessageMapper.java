package com.young.mapper;

import com.young.pojo.SysMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMessageMapper {
    /** 插入站内消息 */
    int insert(SysMessage msg);
    
    /** 查询指定用户的全部消息（按时间倒序） */
    List<SysMessage> selectByUserId(Long userId);
    
    /** 查询指定用户未读消息数量 */
    int countUnread(Long userId);
    
    /** 标记消息为已读 */
    int markRead(@Param("id") Long id, @Param("userId") Long userId);
    
    /** 全部已读 */
    int markAllRead(Long userId);
}
