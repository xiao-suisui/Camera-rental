package com.example.camerarental.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.user.entity.Message;
import com.example.camerarental.user.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    /**
     * 发送消息（内部调用）
     */
    @OperationLog(type = "MESSAGE_SEND", description = "发送站内信")
    public void sendMessage(Long userId, String content, String type) {
        Message message = Message.builder()
                .userId(userId)
                .content(content)
                .type(type)
                .readStatus("UNREAD")
                .createdAt(LocalDateTime.now())
                .build();

        messageMapper.insert(message);
        log.info("Message sent to user {}: {}", userId, content);
    }

    /**
     * 批量发送消息
     */
    public void sendBatchMessage(List<Long> userIds, String content, String type) {
        userIds.forEach(userId -> sendMessage(userId, content, type));
    }

    /**
     * 获取用户消息列表
     */
    public List<Message> listByUserId(Long userId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .orderByDesc(Message::getCreatedAt)
        );
    }

    /**
     * 获取未读消息数量
     */
    public Integer getUnreadCount(Long userId) {
        return Math.toIntExact(messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getReadStatus, "UNREAD")
        ));
    }

    /**
     * 标记消息已读
     */
    @Transactional
    @OperationLog(type = "MESSAGE_READ", description = "标记消息已读")
    public void markRead(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息不存在");
        }
        if (userId == null || !userId.equals(message.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该消息");
        }

        message.setReadStatus("READ");
        messageMapper.updateById(message);
    }

    /**
     * 标记所有消息已读
     */
    @Transactional
    @OperationLog(type = "MESSAGE_READ_ALL", description = "标记所有消息已读")
    public void markAllRead(Long userId) {
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getReadStatus, "UNREAD")
                        .set(Message::getReadStatus, "READ")
        );
    }

    /**
     * 删除消息
     */
    @Transactional
    @OperationLog(type = "MESSAGE_DELETE", description = "删除消息")
    public void delete(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息不存在");
        }
        if (userId == null || !userId.equals(message.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该消息");
        }

        messageMapper.deleteById(messageId);
    }

    /**
     * 删除用户所有消息
     */
    @Transactional
    public void deleteAllByUserId(Long userId) {
        messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
        );
    }
}

