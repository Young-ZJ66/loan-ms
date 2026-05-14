package com.young.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.young.common.Result;
import com.young.mapper.SysMessageMapper;
import com.young.pojo.SysMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 站内消息中心：客户端查收通知、标记已读
 */
@Api(tags = "站内消息管理")
@RestController
@RequestMapping("/api/message")
public class SysMessageController {

    @Autowired
    private SysMessageMapper messageMapper;

    /**
     * 获取当前用户的全部消息
     */
    @ApiOperation("查询我的消息列表")
    @GetMapping("/list")
    public Result<List<SysMessage>> getMyMessages(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageMapper.selectByUserId(userId));
    }

    /**
     * 获取当前用户未读消息数量（用于顶栏角标）
     */
    @ApiOperation("查询未读消息数量")
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageMapper.countUnread(userId));
    }

    /**
     * 标记一条消息为已读
     */
    @ApiOperation("标记单条消息为已读")
    @PutMapping("/read/{id}")
    public Result<String> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messageMapper.markRead(id, userId);
        return Result.success("已标记为已读");
    }

    /**
     * 全部标记为已读
     */
    @ApiOperation("全部消息标记为已读")
    @PutMapping("/read-all")
    public Result<String> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messageMapper.markAllRead(userId);
        return Result.success("全部已读");
    }
}
