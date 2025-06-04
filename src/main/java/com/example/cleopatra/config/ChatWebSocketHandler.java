package com.example.cleopatra.config;

import com.example.cleopatra.dto.ChatMessage.ChatMessage;

import com.example.cleopatra.service.UserOnlineStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final UserOnlineStatusService onlineStatusService;

    // Храним активные сессии пользователей
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // Сопоставление сессия -> userId
    private final ConcurrentHashMap<WebSocketSession, Long> sessionToUserId = new ConcurrentHashMap<>();

    @Autowired
    public ChatWebSocketHandler(UserOnlineStatusService onlineStatusService) {
        this.onlineStatusService = onlineStatusService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            sessionToUserId.put(session, userId);

            // Обновляем статус пользователя на "онлайн"
            onlineStatusService.updateOnlineStatus(userId, true, getClientInfo(session));

            // Уведомляем контакты о том, что пользователь онлайн
            notifyContactsAboutStatusChange(userId, true);

            log.info("User {} connected via WebSocket", userId);
        } else {
            log.warn("Cannot establish connection - userId not found in session");
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            Long senderId = sessionToUserId.get(session);

            if (senderId == null) {
                log.warn("Message from unidentified session: {}", session.getId());
                return;
            }

            log.info("Received message from user {}: {}", senderId, chatMessage.getAction());

            // Обрабатываем различные типы сообщений
            switch (chatMessage.getAction()) {
                case "TYPING_START":
                case "TYPING_STOP":
                    handleTypingNotification(chatMessage);
                    break;
                case "MESSAGE_READ":
                    handleMessageRead(chatMessage);
                    break;
                case "PING":
                    handlePing(session, senderId);
                    break;
                default:
                    // Обычное сообщение - пересылаем получателю
                    sendToUser(chatMessage.getRecipientId(), chatMessage);
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message: ", e);
            session.sendMessage(new TextMessage(
                    "{\"error\":\"Ошибка обработки сообщения\",\"code\":\"PROCESSING_ERROR\"}"
            ));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = sessionToUserId.remove(session);
        if (userId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);

                    // Обновляем статус пользователя на "оффлайн"
                    onlineStatusService.updateOnlineStatus(userId, false, null);

                    // Уведомляем контакты о том, что пользователь офлайн
                    notifyContactsAboutStatusChange(userId, false);
                }
            }
            log.info("User {} disconnected from WebSocket, status: {}", userId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: ", session.getId(), exception);
        sessionToUserId.remove(session);

        for (CopyOnWriteArraySet<WebSocketSession> sessions : userSessions.values()) {
            sessions.remove(session);
        }

        session.close(); // Закрываем соединение
    }

    /**
     * Отправить сообщение конкретному пользователю
     */
    public void sendToUser(Long userId, ChatMessage message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            String messageJson;
            try {
                messageJson = objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                log.error("Error serializing message: ", e);
                return;
            }

            sessions.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(messageJson));
                        log.debug("Message sent to user {} via session {}", userId, session.getId());
                    } else {
                        // Удаляем закрытые сессии
                        sessions.remove(session);
                        sessionToUserId.remove(session);
                    }
                } catch (Exception e) {
                    log.error("Error sending message to user {} via session {}: ",
                            userId, session.getId(), e);
                    // Удаляем проблемную сессию
                    sessions.remove(session);
                    sessionToUserId.remove(session);
                }
            });
        } else {
            log.debug("User {} is not connected via WebSocket", userId);
        }
    }

    /**
     * Отправить сообщение всем подключенным пользователям
     */
    public void broadcast(ChatMessage message) {
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error serializing broadcast message: ", e);
            return;
        }

        userSessions.values().forEach(sessions ->
                sessions.forEach(session -> {
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(messageJson));
                        }
                    } catch (Exception e) {
                        log.error("Error broadcasting message: ", e);
                    }
                })
        );
    }

    /**
     * Проверить, подключен ли пользователь
     */
    public boolean isUserOnline(Long userId) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty() &&
                sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    /**
     * Получить количество активных подключений пользователя
     */
    public int getActiveConnectionsCount(Long userId) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) return 0;
        return (int) sessions.stream().filter(WebSocketSession::isOpen).count();
    }

    /**
     * Получить общее количество активных подключений
     */
    public int getTotalActiveConnections() {
        return userSessions.values().stream()
                .mapToInt(sessions -> (int) sessions.stream()
                        .filter(WebSocketSession::isOpen).count())
                .sum();
    }

    // Приватные методы

    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            // Способ 1: Из query параметров
            String query = session.getUri().getQuery();
            if (query != null && query.contains("userId=")) {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                return Long.parseLong(userIdStr);
            }

            // Способ 2: Из JWT токена (если используется)
            String token = extractTokenFromSession(session);
            if (token != null) {
                return extractUserIdFromToken(token);
            }

            // Способ 3: Из заголовков
            Object userIdHeader = session.getAttributes().get("userId");
            if (userIdHeader != null) {
                return Long.parseLong(userIdHeader.toString());
            }

        } catch (Exception e) {
            log.error("Error extracting userId from session: ", e);
        }
        return null;
    }

    private String extractTokenFromSession(WebSocketSession session) {
        // Извлечение токена из заголовков или параметров
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }
        return null;
    }

    private Long extractUserIdFromToken(String token) {
        // Здесь должна быть логика декодирования JWT токена
        // Для примера возвращаем null
        return null;
    }

    private String getClientInfo(WebSocketSession session) {
        String userAgent = (String) session.getAttributes().get("User-Agent");
        String remoteAddress = session.getRemoteAddress() != null ?
                session.getRemoteAddress().toString() : "unknown";

        return String.format("UserAgent: %s, IP: %s",
                userAgent != null ? userAgent : "unknown",
                remoteAddress);
    }

    private void handleTypingNotification(ChatMessage message) {
        sendToUser(message.getRecipientId(), message);
    }

    private void handleMessageRead(ChatMessage message) {
        sendToUser(message.getSenderId(), message);
    }

    private void handlePing(WebSocketSession session, Long userId) {
        try {
            ChatMessage pong = ChatMessage.builder()
                    .action("PONG")
                    .senderId(userId)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));

            // Обновляем статус активности
            onlineStatusService.updateLastSeen(userId);

        } catch (Exception e) {
            log.error("Error handling ping: ", e);
        }
    }

    private void notifyContactsAboutStatusChange(Long userId, boolean isOnline) {
        // Получаем список контактов пользователя и уведомляем их об изменении статуса
        // Это можно реализовать через сервис контактов
        ChatMessage statusMessage = ChatMessage.builder()
                .action(isOnline ? "USER_ONLINE" : "USER_OFFLINE")
                .senderId(userId)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        // Отправляем уведомление всем контактам (здесь упрощенная версия)
        // В реальном приложении нужно получить список друзей/контактов
        log.info("User {} status changed to: {}", userId, isOnline ? "ONLINE" : "OFFLINE");
    }


}