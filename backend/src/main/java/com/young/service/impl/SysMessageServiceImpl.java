package com.young.service.impl;

import com.young.mapper.SysMessageMapper;
import com.young.pojo.SysMessage;
import com.young.service.SysMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 站内消息服务实现
 */
@Service
public class SysMessageServiceImpl implements SysMessageService {

    @Autowired
    private SysMessageMapper messageMapper;

    @Override
    public List<SysMessage> getMyMessages(Long userId) {
        return messageMapper.selectByUserId(userId);
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        return messageMapper.countUnread(userId);
    }

    @Override
    public void markRead(Long id, Long userId) {
        messageMapper.markRead(id, userId);
    }

    @Override
    public void markAllRead(Long userId) {
        messageMapper.markAllRead(userId);
    }
}
