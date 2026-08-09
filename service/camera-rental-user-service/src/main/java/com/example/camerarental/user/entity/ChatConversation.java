package com.example.camerarental.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("user_id1")
    private Long userId1;

    @TableField("user_id2")
    private Long userId2;

    private Long deviceId;

    private Long orderId;

    private String lastMessage;

    private LocalDateTime lastMessageAt;

    @TableField("unread_count1")
    private Integer unreadCount1;

    @TableField("unread_count2")
    private Integer unreadCount2;

    @TableField("deleted_by_user1")
    private Integer deletedByUser1;

    @TableField("deleted_by_user2")
    private Integer deletedByUser2;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
