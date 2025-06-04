package com.example.cleopatra.config;

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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("🔌 Registering WebSocket handlers");

        // Основной WebSocket endpoint
        registry.addHandler(chatWebSocketHandler, "/ws")
                .setAllowedOriginPatterns("*") // Используем allowedOriginPatterns вместо allowedOrigins
                .withSockJS();

        // Дополнительный endpoint для совместимости
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        log.info("✅ WebSocket handlers registered successfully");
    }
}