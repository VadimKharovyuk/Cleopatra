package com.example.cleopatra.EVENT;

import com.example.cleopatra.config.NotificationWebSocketHandler;
import com.example.cleopatra.model.Notification;
import com.example.cleopatra.repository.NotificationRepository;
import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.maper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
     private final NotificationWebSocketHandler notificationWebSocketHandler;

    @EventListener
    @Async
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        log.info("🎉 EVENT RECEIVED: NotificationCreatedEvent for ID: {} to recipient: {}",
                event.getNotificationId(), event.getRecipientId());

        try {
            // Загружаем уведомление с eager loading
            Notification notification = notificationRepository.findByIdWithUsers(event.getNotificationId())
                    .orElse(null);

            if (notification == null) {
                log.warn("⚠️ Notification not found: {}", event.getNotificationId());
                return;
            }

            log.info("📋 Found notification: title={}", notification.getTitle());

            // Преобразуем в DTO
            NotificationDto dto = notificationMapper.toWebSocketDto(notification);
            log.info("📤 Sending to WebSocket handler for user: {}", event.getRecipientId());

            // 🔧 ИСПОЛЬЗУЕМ ID ИЗ СОБЫТИЯ
            notificationWebSocketHandler.sendNotificationToUser(
                    event.getRecipientId(), // 🔧 Используем ID из события
                    dto
            );

            // Помечаем как отправленное в отдельной транзакции
            updateNotificationAsSent(event.getNotificationId());

            log.info("✅ Notification processing completed for user: {}", event.getRecipientId());

        } catch (Exception e) {
            log.error("❌ Error in event listener for notification: {}", event.getNotificationId(), e);
            e.printStackTrace();
        }
    }

    @Transactional
    public void updateNotificationAsSent(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsSent(true);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.debug("✅ Marked notification {} as sent", notificationId);
        });
    }
}
