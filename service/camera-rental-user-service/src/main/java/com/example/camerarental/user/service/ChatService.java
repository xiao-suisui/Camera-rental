package com.example.camerarental.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.util.ValidationUtils;
import com.example.camerarental.user.dto.ChatConversationDTO;
import com.example.camerarental.user.dto.ChatMessageDTO;
import com.example.camerarental.user.entity.ChatConversation;
import com.example.camerarental.user.entity.ChatMessage;
import com.example.camerarental.user.entity.User;
import com.example.camerarental.user.mapper.ChatConversationMapper;
import com.example.camerarental.user.mapper.ChatMessageMapper;
import com.example.camerarental.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final UserMapper userMapper;

    /**
     * 创建或获取会话
     */
    @Transactional
    @OperationLog(type = "CHAT_CREATE_CONV", description = "创建聊天会话")
    public ChatConversationDTO getOrCreateConversation(Long userId, Long otherUserId,
                                                        Long deviceId, Long orderId) {
        if (userId.equals(otherUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能和自己聊天");
        }

        // 确保 userId1 < userId2
        Long userId1 = Math.min(userId, otherUserId);
        Long userId2 = Math.max(userId, otherUserId);

        ChatConversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<ChatConversation>()
                        .eq(ChatConversation::getUserId1, userId1)
                        .eq(ChatConversation::getUserId2, userId2)
        );

        if (conv == null) {
            conv = ChatConversation.builder()
                    .userId1(userId1)
                    .userId2(userId2)
                    .deviceId(deviceId)
                    .orderId(orderId)
                    .unreadCount1(0)
                    .unreadCount2(0)
                    .deletedByUser1(0)
                    .deletedByUser2(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            conversationMapper.insert(conv);
        } else {
            // 恢复被当前用户删除的会话
            boolean needRestore = false;
            if (userId.equals(conv.getUserId1()) && conv.getDeletedByUser1() == 1) {
                conv.setDeletedByUser1(0);
                needRestore = true;
            } else if (userId.equals(conv.getUserId2()) && conv.getDeletedByUser2() == 1) {
                conv.setDeletedByUser2(0);
                needRestore = true;
            }
            if (needRestore) {
                conversationMapper.updateById(conv);
            }
        }

        return toConversationDTO(conv, userId);
    }

    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * 发送消息
     */
    @Transactional
    public ChatMessage sendMessage(Long conversationId, Long senderId, Long receiverId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }
        content = ValidationUtils.sanitizeHtml(content.trim());
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息内容不能超过" + MAX_MESSAGE_LENGTH + "字");
        }

        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }

        // 校验发送者是会话参与者
        if (!senderId.equals(conv.getUserId1()) && !senderId.equals(conv.getUserId2())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权在该会话中发送消息");
        }

        // 校验接收者是会话另一方参与者
        Long expectedReceiver = senderId.equals(conv.getUserId1()) ? conv.getUserId2() : conv.getUserId1();
        if (!expectedReceiver.equals(receiverId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "接收者不是该会话的参与者");
        }

        // 创建消息
        ChatMessage message = ChatMessage.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .isRead(0)
                .createdAt(LocalDateTime.now())
                .build();
        messageMapper.insert(message);

        // 原子更新会话：最后消息 + 未读计数
        String lastMsg = content.length() > 500 ? content.substring(0, 500) : content;
        if (senderId.equals(conv.getUserId1())) {
            conversationMapper.update(null,
                    new LambdaUpdateWrapper<ChatConversation>()
                            .eq(ChatConversation::getId, conversationId)
                            .set(ChatConversation::getLastMessage, lastMsg)
                            .set(ChatConversation::getLastMessageAt, LocalDateTime.now())
                            .setSql("unread_count_2 = unread_count_2 + 1")
            );
        } else {
            conversationMapper.update(null,
                    new LambdaUpdateWrapper<ChatConversation>()
                            .eq(ChatConversation::getId, conversationId)
                            .set(ChatConversation::getLastMessage, lastMsg)
                            .set(ChatConversation::getLastMessageAt, LocalDateTime.now())
                            .setSql("unread_count_1 = unread_count_1 + 1")
            );
        }

        return message;
    }

    /**
     * 获取用户的会话列表
     */
    public List<ChatConversationDTO> listConversations(Long userId) {
        List<ChatConversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<ChatConversation>()
                        .and(w -> w
                                .and(inner -> inner
                                        .eq(ChatConversation::getUserId1, userId)
                                        .eq(ChatConversation::getDeletedByUser1, 0)
                                )
                                .or()
                                .and(inner -> inner
                                        .eq(ChatConversation::getUserId2, userId)
                                        .eq(ChatConversation::getDeletedByUser2, 0)
                                )
                        )
                        .orderByDesc(ChatConversation::getLastMessageAt)
        );

        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询对方用户信息
        List<Long> otherUserIds = conversations.stream()
                .map(c -> c.getUserId1().equals(userId) ? c.getUserId2() : c.getUserId1())
                .distinct()
                .collect(Collectors.toList());

        List<User> users = userMapper.selectBatchIds(otherUserIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ChatConversationDTO> result = new ArrayList<>();
        for (ChatConversation conv : conversations) {
            Long otherUserId = conv.getUserId1().equals(userId)
                    ? conv.getUserId2() : conv.getUserId1();
            User otherUser = userMap.get(otherUserId);
            int unread = conv.getUserId1().equals(userId)
                    ? conv.getUnreadCount1() : conv.getUnreadCount2();

            result.add(ChatConversationDTO.builder()
                    .id(conv.getId())
                    .otherUserId(otherUserId)
                    .otherNickname(otherUser != null ? otherUser.getNickname() : "未知用户")
                    .otherAvatar(otherUser != null ? otherUser.getAvatar() : null)
                    .deviceId(conv.getDeviceId())
                    .orderId(conv.getOrderId())
                    .lastMessage(conv.getLastMessage())
                    .lastMessageAt(conv.getLastMessageAt())
                    .unreadCount(unread)
                    .createdAt(conv.getCreatedAt())
                    .build());
        }

        return result;
    }

    /**
     * 获取会话的历史消息（分页，倒序）
     */
    public List<ChatMessageDTO> listMessages(Long conversationId, Long userId,
                                              int page, int size) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (!userId.equals(conv.getUserId1()) && !userId.equals(conv.getUserId2())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该会话");
        }

        Page<ChatMessage> pageParam = new Page<>(page, size);
        List<ChatMessage> messages = messageMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByDesc(ChatMessage::getCreatedAt)
        ).getRecords();

        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询发送者信息
        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId).distinct().collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(senderIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 按时间正序返回
        Collections.reverse(messages);

        return messages.stream().map(msg -> {
            User sender = userMap.get(msg.getSenderId());
            return ChatMessageDTO.builder()
                    .id(msg.getId())
                    .conversationId(msg.getConversationId())
                    .senderId(msg.getSenderId())
                    .senderNickname(sender != null ? sender.getNickname() : "未知用户")
                    .senderAvatar(sender != null ? sender.getAvatar() : null)
                    .receiverId(msg.getReceiverId())
                    .content(msg.getContent())
                    .isRead(msg.getIsRead())
                    .createdAt(msg.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 标记会话消息已读
     */
    @Transactional
    @OperationLog(type = "CHAT_READ", description = "标记聊天已读")
    public void markAsRead(Long conversationId, Long userId) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (!userId.equals(conv.getUserId1()) && !userId.equals(conv.getUserId2())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该会话");
        }

        // 将对方发给自己的消息标记为已读
        messageMapper.update(null,
                new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .eq(ChatMessage::getReceiverId, userId)
                        .eq(ChatMessage::getIsRead, 0)
                        .set(ChatMessage::getIsRead, 1)
        );

        // 原子清零未读计数
        if (userId.equals(conv.getUserId1())) {
            conversationMapper.update(null,
                    new LambdaUpdateWrapper<ChatConversation>()
                            .eq(ChatConversation::getId, conversationId)
                            .set(ChatConversation::getUnreadCount1, 0)
            );
        } else {
            conversationMapper.update(null,
                    new LambdaUpdateWrapper<ChatConversation>()
                            .eq(ChatConversation::getId, conversationId)
                            .set(ChatConversation::getUnreadCount2, 0)
            );
        }
    }

    /**
     * 获取用户总未读消息数
     */
    public Integer getTotalUnreadCount(Long userId) {
        List<ChatConversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<ChatConversation>()
                        .and(w -> w
                                .and(inner -> inner
                                        .eq(ChatConversation::getUserId1, userId)
                                        .eq(ChatConversation::getDeletedByUser1, 0)
                                )
                                .or()
                                .and(inner -> inner
                                        .eq(ChatConversation::getUserId2, userId)
                                        .eq(ChatConversation::getDeletedByUser2, 0)
                                )
                        )
        );

        int total = 0;
        for (ChatConversation conv : conversations) {
            if (conv.getUserId1().equals(userId)) {
                total += conv.getUnreadCount1();
            } else {
                total += conv.getUnreadCount2();
            }
        }
        return total;
    }

    /**
     * 删除会话（软删除当前用户视角）
     */
    @Transactional
    @OperationLog(type = "CHAT_DELETE_CONV", description = "删除聊天会话")
    public void deleteConversation(Long conversationId, Long userId) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (userId.equals(conv.getUserId1())) {
            conv.setDeletedByUser1(1);
        } else if (userId.equals(conv.getUserId2())) {
            conv.setDeletedByUser2(1);
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该会话");
        }
        conversationMapper.updateById(conv);

        // 双方都删除时物理删除
        if (Integer.valueOf(1).equals(conv.getDeletedByUser1()) && Integer.valueOf(1).equals(conv.getDeletedByUser2())) {
            messageMapper.delete(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getConversationId, conversationId)
            );
            conversationMapper.deleteById(conversationId);
        }
    }

    /**
     * 转换为 DTO
     */
    private ChatConversationDTO toConversationDTO(ChatConversation conv, Long userId) {
        Long otherUserId = conv.getUserId1().equals(userId)
                ? conv.getUserId2() : conv.getUserId1();
        User otherUser = userMapper.selectById(otherUserId);
        int unread = conv.getUserId1().equals(userId)
                ? conv.getUnreadCount1() : conv.getUnreadCount2();

        return ChatConversationDTO.builder()
                .id(conv.getId())
                .otherUserId(otherUserId)
                .otherNickname(otherUser != null ? otherUser.getNickname() : "未知用户")
                .otherAvatar(otherUser != null ? otherUser.getAvatar() : null)
                .deviceId(conv.getDeviceId())
                .orderId(conv.getOrderId())
                .lastMessage(conv.getLastMessage())
                .lastMessageAt(conv.getLastMessageAt())
                .unreadCount(unread)
                .createdAt(conv.getCreatedAt())
                .build();
    }
}
