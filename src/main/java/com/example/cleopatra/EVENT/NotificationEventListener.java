package com.example.cleopatra.EVENT;

import com.example.cleopatra.config.NotificationWebSocketHandler;
import com.example.cleopatra.model.Notification;
import com.example.cleopatra.model.User;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

            // 🔧 ПОПЫТКА ОТПРАВКИ ЧЕРЕЗ WEBSOCKET
            boolean sentViaWebSocket = notificationWebSocketHandler.sendNotificationToUser(
                    event.getRecipientId(),
                    dto
            );

            if (sentViaWebSocket) {
                // ✅ Отправлено через WebSocket
                log.info("📤 Notification sent via WebSocket to user: {}", event.getRecipientId());
                updateNotificationAsSent(event.getNotificationId());
            } else {
                // 🔕 Пользователь оффлайн - проверяем его статус в БД
                log.info("🔕 User {} not connected to WebSocket, checking online status", event.getRecipientId());

                User recipient = notification.getRecipient();
                if (Boolean.TRUE.equals(recipient.getIsOnline())) {
                    // Пользователь онлайн в БД, но не подключен к WebSocket
                    log.info("👤 User {} is online but not connected to WebSocket, will retry later", event.getRecipientId());

                    // Планируем повторную отправку через 30 секунд
                    scheduleRetryNotification(event.getNotificationId(), event.getRecipientId(), dto);
                } else {
                    log.info("💤 User {} is offline, notification will be delivered when online", event.getRecipientId());
                    // Уведомление останется в БД как неотправленное
                }
            }

            log.info("✅ Notification processing completed for user: {}", event.getRecipientId());

        } catch (Exception e) {
            log.error("❌ Error in event listener for notification: {}", event.getNotificationId(), e);
            e.printStackTrace();
        }
    }

    /**
     * Планирует повторную отправку уведомления
     */
    private void scheduleRetryNotification(Long notificationId, Long recipientId, NotificationDto dto) {
        log.info("⏰ Scheduling retry for notification {} to user {} in 30 seconds", notificationId, recipientId);

        // Используем CompletableFuture для отложенной отправки
        CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
            try {
                log.info("🔄 Retrying notification delivery for user: {}", recipientId);

                boolean sentViaWebSocket = notificationWebSocketHandler.sendNotificationToUser(recipientId, dto);

                if (sentViaWebSocket) {
                    log.info("✅ Notification delivered on retry to user: {}", recipientId);
                    updateNotificationAsSent(notificationId);
                } else {
                    log.info("🔕 Retry failed - user {} still not connected", recipientId);
                    // Можно запланировать еще одну попытку или оставить как есть
                }

            } catch (Exception e) {
                log.error("❌ Error during notification retry for user: {}", recipientId, e);
            }
        });
    }

//    @EventListener
//    @Async
//    public void handleNotificationCreated(NotificationCreatedEvent event) {
//        log.info("🎉 EVENT RECEIVED: NotificationCreatedEvent for ID: {} to recipient: {}",
//                event.getNotificationId(), event.getRecipientId());
//
//        try {
//            // Загружаем уведомление с eager loading
//            Notification notification = notificationRepository.findByIdWithUsers(event.getNotificationId())
//                    .orElse(null);
//
//            if (notification == null) {
//                log.warn("⚠️ Notification not found: {}", event.getNotificationId());
//                return;
//            }
//
//            log.info("📋 Found notification: title={}", notification.getTitle());
//
//            // Преобразуем в DTO
//            NotificationDto dto = notificationMapper.toWebSocketDto(notification);
//            log.info("📤 Sending to WebSocket handler for user: {}", event.getRecipientId());
//
//            // 🔧 ИСПОЛЬЗУЕМ ID ИЗ СОБЫТИЯ
//            notificationWebSocketHandler.sendNotificationToUser(
//                    event.getRecipientId(), // 🔧 Используем ID из события
//                    dto
//            );
//
//            // Помечаем как отправленное в отдельной транзакции
//            updateNotificationAsSent(event.getNotificationId());
//
//            log.info("✅ Notification processing completed for user: {}", event.getRecipientId());
//
//        } catch (Exception e) {
//            log.error("❌ Error in event listener for notification: {}", event.getNotificationId(), e);
//            e.printStackTrace();
//        }
//    }
//
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
