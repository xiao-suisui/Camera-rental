package com.example.camerarental.user.controller;

import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.user.dto.ChatConversationDTO;
import com.example.camerarental.user.dto.ChatMessageDTO;
import com.example.camerarental.user.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 创建或获取会话
     */
    @PostMapping("/conversations")
    public Result<ChatConversationDTO> getOrCreateConversation(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> body) {
        Object otherUserIdObj = body.get("otherUserId");
        if (otherUserIdObj == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "otherUserId 不能为空");
        }
        Long otherUserId;
        try {
            otherUserId = Long.valueOf(otherUserIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "otherUserId 格式错误");
        }
        if (userId.equals(otherUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能和自己创建会话");
        }
        Long deviceId = body.get("deviceId") != null ? Long.valueOf(body.get("deviceId").toString()) : null;
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        return Result.success(chatService.getOrCreateConversation(userId, otherUserId, deviceId, orderId));
    }

    /**
     * 获取我的会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ChatConversationDTO>> listConversations(
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(chatService.listConversations(userId));
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<ChatMessageDTO>> listMessages(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(chatService.listMessages(conversationId, userId, page, size));
    }

    /**
     * 获取总未读消息数
     */
    @GetMapping("/unread-total")
    public Result<Integer> getUnreadTotal(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(chatService.getTotalUnreadCount(userId));
    }

    /**
     * 标记会话已读
     */
    @PostMapping("/conversations/{conversationId}/read")
    public Result<Void> markAsRead(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId) {
        chatService.markAsRead(conversationId, userId);
        return Result.success("标记已读成功", null);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId) {
        chatService.deleteConversation(conversationId, userId);
        return Result.success("删除会话成功", null);
    }
}
