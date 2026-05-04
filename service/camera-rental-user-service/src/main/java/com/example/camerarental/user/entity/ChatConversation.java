package com.example.camerarental.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId1;

    private Long userId2;

    private Long deviceId;

    private Long orderId;

    private String lastMessage;

    private LocalDateTime lastMessageAt;

    private Integer unreadCount1;

    private Integer unreadCount2;

    private Integer deletedByUser1;

    private Integer deletedByUser2;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
