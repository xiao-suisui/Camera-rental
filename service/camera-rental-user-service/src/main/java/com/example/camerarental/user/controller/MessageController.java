package com.example.camerarental.user.controller;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.user.entity.Message;
import com.example.camerarental.user.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 获取消息列表
     */
    @GetMapping("/list")
    public Result<List<Message>> list(@RequestParam Long userId) {
        return Result.success(messageService.listByUserId(userId));
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(@RequestParam Long userId) {
        return Result.success(messageService.getUnreadCount(userId));
    }

    /**
     * 标记消息已读
     */
    @PostMapping("/read/{messageId}")
    public Result<Void> markRead(@PathVariable Long messageId) {
        messageService.markRead(messageId);
        return Result.success("标记已读成功", null);
    }

    /**
     * 标记所有消息已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllRead(@RequestParam Long userId) {
        messageService.markAllRead(userId);
        return Result.success("全部已读", null);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    public Result<Void> delete(@PathVariable Long messageId) {
        messageService.delete(messageId);
        return Result.success("删除成功", null);
    }

    /**
     * 发送消息（内部接口，供其他服务调用）
     */
    @PostMapping("/send")
    public Result<Void> sendMessage(
            @RequestParam Long userId,
            @RequestParam String content,
            @RequestParam String type) {
        messageService.sendMessage(userId, content, type);
        return Result.success(null);
    }
}

