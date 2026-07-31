package com.young.controller;

import com.young.common.Result;
import com.young.pojo.SysMessage;
import com.young.service.SysMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内消息中心：客户端查收通知、标记已读
 */
@Tag(name = "站内消息管理")
@RestController
@RequestMapping("/api/message")
public class SysMessageController {

    @Autowired
    private SysMessageService messageService;

    /**
     * 获取当前用户的全部消息
     */
    @Operation(summary = "查询我的消息列表")
    @GetMapping("/list")
    public Result<List<SysMessage>> getMyMessages(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getMyMessages(userId));
    }

    /**
     * 获取当前用户未读消息数量（用于顶栏角标）
     */
    @Operation(summary = "查询未读消息数量")
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getUnreadCount(userId));
    }

    /**
     * 标记一条消息为已读
     */
    @Operation(summary = "标记单条消息为已读")
    @PutMapping("/read/{id}")
    public Result<String> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messageService.markRead(id, userId);
        return Result.success("已标记为已读");
    }

    /**
     * 全部标记为已读
     */
    @Operation(summary = "全部消息标记为已读")
    @PutMapping("/read-all")
    public Result<String> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messageService.markAllRead(userId);
        return Result.success("全部已读");
    }
}
