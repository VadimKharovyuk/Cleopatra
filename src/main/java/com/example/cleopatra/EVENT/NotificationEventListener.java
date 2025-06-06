package com.example.cleopatra.EVENT;

import com.example.cleopatra.model.Notification;
import com.example.cleopatra.repository.NotificationRepository;
import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.maper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    // private final WebSocketNotificationService webSocketService; // Добавите позже

    @EventListener
    @Async
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        log.debug("📨 Handling notification created event: {}", event.getNotificationId());

        try {
            Notification notification = notificationRepository.findById(event.getNotificationId())
                    .orElse(null);

            if (notification == null) {
                log.warn("⚠️ Notification not found: {}", event.getNotificationId());
                return;
            }

            // Проверяем, что пользователь онлайн
            if (Boolean.TRUE.equals(notification.getRecipient().getIsOnline())) {
                NotificationDto dto = notificationMapper.toWebSocketDto(notification);

                // TODO: Отправляем через WebSocket когда будет готов
                log.info("📤 Would send real-time notification to user {} (WebSocket not implemented yet)",
                        notification.getRecipient().getId());

                // Помечаем как отправленное
                notification.setIsSent(true);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.debug("✅ Marked notification as sent: {}", event.getNotificationId());
            } else {
                log.debug("🔕 User {} is offline, notification will be sent later",
                        notification.getRecipient().getId());
            }

        } catch (Exception e) {
            log.error("❌ Error handling notification created event: {}", event.getNotificationId(), e);
        }
    }
}
