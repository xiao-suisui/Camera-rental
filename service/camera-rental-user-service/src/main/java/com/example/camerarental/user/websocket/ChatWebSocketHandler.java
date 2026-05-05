package com.example.camerarental.user.websocket;

import com.example.camerarental.user.config.JwtService;
import com.example.camerarental.user.dto.ChatMessageDTO;
import com.example.camerarental.user.entity.ChatMessage;
import com.example.camerarental.user.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 60;

    private final Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastPongTime = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ChatService chatService;
    private final JwtService jwtService;

    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledFuture<?> heartbeatTask;

    public ChatWebSocketHandler(ChatService chatService, JwtService jwtService) {
        this.chatService = chatService;
        this.jwtService = jwtService;
    }

    @PostConstruct
    public void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ws-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
                this::sendPingAndCleanup,
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @PreDestroy
    public void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            Long userId = extractUserId(session);
            if (userId == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("认证失败"));
                return;
            }
            // 关闭旧连接（同用户多设备场景）
            WebSocketSession oldSession = onlineUsers.put(userId, session);
            if (oldSession != null && oldSession.isOpen()) {
                oldSession.close(CloseStatus.NORMAL.withReason("新连接替换"));
            }
            lastPongTime.put(userId, System.currentTimeMillis());
            log.info("WebSocket connected: userId={}", userId);
        } catch (Exception e) {
            log.error("WebSocket connection error", e);
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("认证失败"));
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = extractUserId(session);
        if (senderId == null) {
            return;
        }

        JsonNode json;
        try {
            json = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            return;
        }

        // 处理 pong 心跳响应
        String type = json.has("type") ? json.get("type").asText() : null;
        if ("pong".equals(type)) {
            lastPongTime.put(senderId, System.currentTimeMillis());
            return;
        }

        JsonNode convIdNode = json.get("conversationId");
        JsonNode receiverIdNode = json.get("receiverId");
        if (convIdNode == null || receiverIdNode == null) {
            return;
        }

        Long conversationId = convIdNode.asLong();
        Long receiverId = receiverIdNode.asLong();
        String content = json.has("content") ? json.get("content").asText().trim() : null;

        if (content == null || content.isEmpty() || content.length() > MAX_CONTENT_LENGTH) {
            return;
        }

        // 存库
        ChatMessage chatMessage = chatService.sendMessage(conversationId, senderId, receiverId, content);

        // 构建返回消息（使用 DTO 格式，与 REST API 一致）
        ChatMessageDTO dto = ChatMessageDTO.builder()
                .id(chatMessage.getId())
                .conversationId(chatMessage.getConversationId())
                .senderId(chatMessage.getSenderId())
                .receiverId(chatMessage.getReceiverId())
                .content(chatMessage.getContent())
                .isRead(chatMessage.getIsRead())
                .createdAt(chatMessage.getCreatedAt())
                .build();
        String responseJson = objectMapper.writeValueAsString(dto);

        // 推送给发送者确认
        session.sendMessage(new TextMessage(responseJson));

        // 推送给接收者（如果在线）
        WebSocketSession receiverSession = onlineUsers.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage(responseJson));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            onlineUsers.remove(userId);
            lastPongTime.remove(userId);
            log.info("WebSocket disconnected: userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", exception.getMessage());
        Long userId = extractUserId(session);
        if (userId != null) {
            onlineUsers.remove(userId);
            lastPongTime.remove(userId);
        }
    }

    private Long extractUserId(WebSocketSession session) {
        // Read token from query parameter: /ws/chat?token=xxx
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        String token = null;
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    token = param.substring("token=".length());
                    break;
                }
            }
        }
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = jwtService.parseTokenStrict(token);
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.warn("Invalid WebSocket token: {}", e.getMessage());
            return null;
        }
    }

    private void sendPingAndCleanup() {
        String pingJson = "{\"type\":\"ping\"}";
        long now = System.currentTimeMillis();

        onlineUsers.forEach((userId, session) -> {
            try {
                // 检查 pong 超时
                Long lastPong = lastPongTime.get(userId);
                if (lastPong != null && (now - lastPong) > HEARTBEAT_TIMEOUT_SECONDS * 1000) {
                    log.info("Heartbeat timeout, closing session: userId={}", userId);
                    try {
                        session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("心跳超时"));
                    } catch (IOException ignored) {
                    }
                    onlineUsers.remove(userId);
                    lastPongTime.remove(userId);
                    return;
                }
                // 发送 ping
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(pingJson));
                }
            } catch (IOException e) {
                log.warn("Failed to ping userId={}, closing: {}", userId, e.getMessage());
                try {
                    session.close();
                } catch (IOException ignored) {
                }
                onlineUsers.remove(userId);
                lastPongTime.remove(userId);
            }
        });
    }
}
