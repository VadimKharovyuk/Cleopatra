package com.example.cleopatra.config;//package com.example.cleopatra.config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        // Основные endpoints с SockJS
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addHandler(chatWebSocketHandler, "/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Прямые WebSocket endpoints (без SockJS)
        registry.addHandler(chatWebSocketHandler, "/ws/chat/websocket")
                .setAllowedOriginPatterns("*");

        registry.addHandler(chatWebSocketHandler, "/ws/websocket")
                .setAllowedOriginPatterns("*");

        // ===== NOTIFICATION ENDPOINTS ===== 🆕
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addHandler(notificationWebSocketHandler, "/ws/notifications/websocket")
                .setAllowedOriginPatterns("*");

    }
}