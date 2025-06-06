package com.example.cleopatra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableScheduling
@SpringBootApplication
@EnableAsync
public class CleopatraApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleopatraApplication.class, args);





//// Подключение к WebSocket
//const socket = new WebSocket('/ws/notifications');
//        socket.onmessage = (event) => {
//    const notification = JSON.parse(event.data);
//            showNotificationPopup(notification);
//        };


//        @Override
//        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//            registry.addHandler(chatWebSocketHandler, "/chat")
//                    .setAllowedOrigins("*") // В продакшене указать конкретные домены
//                    .withSockJS(); // Fallback для старых браузеров
//        }

    }

}
