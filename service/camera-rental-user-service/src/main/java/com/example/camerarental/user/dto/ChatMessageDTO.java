package com.example.camerarental.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private Long receiverId;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;
}
