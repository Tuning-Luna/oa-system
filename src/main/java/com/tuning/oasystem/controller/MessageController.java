package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.MessageQuery;
import com.tuning.oasystem.security.LoginUser;
import com.tuning.oasystem.service.MessageService;
import com.tuning.oasystem.vo.SysMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统通知接口
 */
@Tag(name = "系统通知", description = "我的通知分页查询 / 标记已读 / 未读计数")
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "我的通知分页查询", description = "按未读状态筛选可选")
    @GetMapping
    public Result<PageResult<SysMessageVO>> page(@AuthenticationPrincipal LoginUser loginUser, MessageQuery query) {
        return Result.success(messageService.page(loginUser.getUserId(), query));
    }

    @Operation(summary = "未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(messageService.unreadCount(loginUser.getUserId()));
    }

    @Operation(summary = "标记单条已读", description = "仅本人通知可操作")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        messageService.markRead(loginUser.getUserId(), id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal LoginUser loginUser) {
        messageService.markAllRead(loginUser.getUserId());
        return Result.success();
    }
}
