package com.example.cleopatra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Храним сессии пользователей
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🔌 WebSocket connection established: {}", session.getId());

        // Отправляем подтверждение подключения
        sendMessage(session, Map.of(
                "action", "CONNECT_ACK",
                "message", "Connected successfully"
        ));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            String payload = message.getPayload().toString();
            log.debug("📨 Received WebSocket message: {}", payload);

            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            String action = (String) data.get("action");

            if ("IDENTITY".equals(type) && "CONNECT".equals(action)) {
                Object userIdObj = data.get("userId");
                if (userIdObj != null) {
                    Long userId = Long.valueOf(userIdObj.toString());
                    userSessions.put(userId, session);
                    session.getAttributes().put("userId", userId);
                    log.info("✅ User {} connected via WebSocket", userId);

                    // Отправляем подтверждение
                    sendMessage(session, Map.of(
                            "action", "IDENTITY_ACK",
                            "userId", userId,
                            "message", "Identity confirmed"
                    ));
                }
            }

        } catch (Exception e) {
            log.error("❌ Error handling WebSocket message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            log.info("🔌 User {} disconnected from WebSocket", userId);
        }
        log.info("🔌 WebSocket connection closed: {} - {}", session.getId(), closeStatus.toString());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Отправить сообщение конкретному пользователю
     */
    public void sendToUser(Long userId, Object message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, message);
                log.debug("📤 Message sent to user {}: {}", userId, message);
            } catch (Exception e) {
                log.error("❌ Error sending message to user {}: {}", userId, e.getMessage());
                // Удаляем неработающую сессию
                userSessions.remove(userId);
            }
        } else {
            log.debug("👤 User {} is not connected via WebSocket", userId);
        }
    }

    /**
     * Отправить сообщение всем подключенным пользователям
     */
    public void sendToAll(Object message) {
        userSessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("❌ Error sending broadcast message to user {}: {}", userId, e.getMessage());
                    userSessions.remove(userId);
                }
            } else {
                userSessions.remove(userId);
            }
        });
    }

    /**
     * Проверить, подключен ли пользователь
     */
    public boolean isUserConnected(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * Получить количество подключенных пользователей
     */
    public int getConnectedUsersCount() {
        return (int) userSessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }

    /**
     * Отправить JSON сообщение в сессию
     */
    private void sendMessage(WebSocketSession session, Object message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }
}