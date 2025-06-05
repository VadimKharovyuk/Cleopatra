package com.example.cleopatra.config;

import com.example.cleopatra.config.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketMonitor {

    public WebSocketMonitor(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    private final ChatWebSocketHandler chatWebSocketHandler;

//    @Scheduled(fixedRate = 60000) // каждые 60 секунд
//    public void logConnectionStats() {
//        int totalConnections = chatWebSocketHandler.getConnectedUsersCount();
//        log.info("Total active WebSocket connections: {}", totalConnections);
//    }
}
