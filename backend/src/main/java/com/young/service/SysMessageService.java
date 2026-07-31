package com.young.service;

import com.young.pojo.SysMessage;

import java.util.List;

/**
 * 站内消息服务接口
 */
public interface SysMessageService {

    /** 获取当前用户的全部消息 */
    List<SysMessage> getMyMessages(Long userId);

    /** 获取当前用户未读消息数量 */
    Integer getUnreadCount(Long userId);

    /** 标记一条消息为已读 */
    void markRead(Long id, Long userId);

    /** 全部标记为已读 */
    void markAllRead(Long userId);
}
