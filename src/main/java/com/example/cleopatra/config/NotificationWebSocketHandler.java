package com.example.cleopatra.config;

import com.example.cleopatra.dto.Notification.NotificationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;

    // Отдельное хранилище для уведомлений
    private final Map<Long, WebSocketSession> notificationSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🔔 Notification WebSocket connection established: {}", session.getId());

        // Получаем userId из URL параметров
        Long userId = getUserIdFromSession(session);

        if (userId != null) {
            notificationSessions.put(userId, session);
            session.getAttributes().put("userId", userId);
            log.info("👤 User {} connected to notifications WebSocket", userId);

            // Отправляем подтверждение
            sendMessage(session, Map.of(
                    "type", "connected",
                    "message", "Connected to notifications"
            ));
        } else {
            log.warn("⚠️ No user ID found, closing connection");
            session.close();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // Для уведомлений обычно не нужна сложная логика
        log.debug("📨 Received notification message: {}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ Notification WebSocket error for session {}: {}",
                session.getId(), exception.getMessage());
        removeUserSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            notificationSessions.remove(userId);
            log.info("🔔 User {} disconnected from notifications WebSocket", userId);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // ===================== МЕТОДЫ ДЛЯ УВЕДОМЛЕНИЙ =====================

    /**
     * 🚀 ГЛАВНЫЙ МЕТОД - отправка уведомления
     */

    /**
     * Отправить уведомление пользователю
     * @return true если отправлено успешно, false если пользователь не подключен
     */
    public boolean sendNotificationToUser(Long userId, NotificationDto notification) {
        log.debug("📤 Attempting to send notification to user {}: {}", userId, notification.getTitle());

        WebSocketSession session = notificationSessions.get(userId);

        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                        "type", "new_notification",
                        "notification", notification
                );

                sendMessage(session, message);
                log.info("✅ Notification sent to user {}: {}", userId, notification.getTitle());
                return true; // 🔧 Возвращаем true при успехе

            } catch (Exception e) {
                log.error("❌ Error sending notification to user {}", userId, e);
                notificationSessions.remove(userId);
                return false; // 🔧 Возвращаем false при ошибке
            }
        } else {
            log.debug("🔕 User {} not connected to notifications WebSocket", userId);
            return false; // 🔧 Возвращаем false если не подключен
        }
    }


    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null && query.contains("userId=")) {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                return Long.parseLong(userIdStr);
            }
        } catch (Exception e) {
            log.error("❌ Error extracting user ID", e);
        }
        return null;
    }

    private void sendMessage(WebSocketSession session, Object message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            log.debug("📨 Message sent: {}", json);
        }
    }

    /**
     * Проверить подключен ли пользователь к уведомлениям
     */
    public boolean isUserConnected(Long userId) {
        WebSocketSession session = notificationSessions.get(userId);
        return session != null && session.isOpen();
    }

    // ===================== ПРИВАТНЫЕ МЕТОДЫ =====================



    private void removeUserSession(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            notificationSessions.remove(userId);
        }
    }
}