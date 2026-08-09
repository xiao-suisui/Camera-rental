package com.example.camerarental.user.controller;

import com.example.camerarental.common.annotation.AdminOnly;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.user.entity.Message;
import com.example.camerarental.user.service.MessageService;
import com.example.camerarental.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    /**
     * 获取消息列表
     */
    @GetMapping("/list")
    public Result<List<Message>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(messageService.listByUserId(userId));
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(messageService.getUnreadCount(userId));
    }

    /**
     * 标记消息已读
     */
    @PostMapping("/read/{messageId}")
    public Result<Void> markRead(
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId) {
        messageService.markRead(messageId, userId);
        return Result.success("标记已读成功", null);
    }

    /**
     * 标记所有消息已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        messageService.markAllRead(userId);
        return Result.success("全部已读", null);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    public Result<Void> delete(
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId) {
        messageService.delete(messageId, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 发送消息（内部接口，供其他服务调用）
     */
    @AdminOnly
    @PostMapping("/send")
    public Result<Void> sendMessage(
            @RequestParam Long userId,
            @RequestParam String content,
            @RequestParam String type) {
        messageService.sendMessage(userId, content, type);
        return Result.success(null);
    }

    /**
     * 内部接口：发送消息（供其他服务Feign调用，无需鉴权）
     */
    @PostMapping("/internal/send")
    public Result<Void> sendInternalMessage(
            @RequestParam Long userId,
            @RequestParam String content,
            @RequestParam String type) {
        messageService.sendMessage(userId, content, type);
        return Result.success(null);
    }

    /**
     * 内部接口：向所有管理员发送消息
     */
    @PostMapping("/internal/send-to-admins")
    public Result<Void> sendToAllAdmins(
            @RequestParam String content,
            @RequestParam String type) {
        List<Long> adminIds = userService.getAdminUserIds();
        messageService.sendBatchMessage(adminIds, content, type);
        return Result.success(null);
    }
}

