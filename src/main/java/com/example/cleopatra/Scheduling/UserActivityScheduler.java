package com.example.cleopatra.Scheduling;

import com.example.cleopatra.config.NotificationWebSocketHandler;
import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.maper.NotificationMapper;
import com.example.cleopatra.model.Notification;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.NotificationRepository;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
@Component
@Slf4j
@RequiredArgsConstructor
public class UserActivityScheduler {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationMapper notificationMapper;

    // Конфигурация
    private static final int OFFLINE_THRESHOLD_MINUTES = 10;
    private static final int MAX_BATCH_SIZE = 1000;

    @Scheduled(fixedRate = 300000) // 5 минут
    public void markInactiveUsersOffline() {
        LocalDateTime threshold = LocalDateTime.now().minus(OFFLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);

        // Статистика перед обработкой
        long totalOnline = userRepository.countOnlineUsers();
        long totalInactive = userRepository.countInactiveUsers(threshold);

        if (totalInactive == 0) {
            log.debug("✅ No inactive users found (total online: {})", totalOnline);
            return;
        }

        log.info("📊 Found {} inactive users out of {} online users", totalInactive, totalOnline);

        // Батчевая обработка
        int totalUpdated = 0;
        int batchNumber = 1;
        int batchUpdated;

        do {
            batchUpdated = userRepository.markUsersOfflineByLastActivityBatch(threshold, MAX_BATCH_SIZE);
            totalUpdated += batchUpdated;

            if (batchUpdated > 0) {
                log.debug("📦 Batch {}: processed {} users", batchNumber, batchUpdated);
                batchNumber++;
            }

        } while (batchUpdated == MAX_BATCH_SIZE);

        log.info("📴 Total marked offline: {} users in {} batches", totalUpdated, batchNumber - 1);
    }



    /**
     * Отправляет накопившиеся уведомления пользователям когда они приходят онлайн
     */
    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void sendPendingNotifications() {
        log.debug("🔄 Checking for pending notifications...");

        try {
            // Находим неотправленные уведомления для онлайн пользователей
            List<Notification> pendingNotifications = notificationRepository.findPendingNotificationsForOnlineUsers();

            if (!pendingNotifications.isEmpty()) {
                log.info("📦 Found {} pending notifications for online users", pendingNotifications.size());

                for (Notification notification : pendingNotifications) {
                    try {
                        Long recipientId = notification.getRecipient().getId();

                        // Проверяем что пользователь подключен к WebSocket
                        if (notificationWebSocketHandler.isUserConnected(recipientId)) {
                            NotificationDto dto = notificationMapper.toWebSocketDto(notification);

                            boolean sent = notificationWebSocketHandler.sendNotificationToUser(recipientId, dto);
                            if (sent) {
                                updateNotificationAsSent(notification.getId());
                                log.info("📤 Delivered pending notification {} to user {}", notification.getId(), recipientId);
                            }
                        }

                    } catch (Exception e) {
                        log.error("❌ Error sending pending notification: {}", notification.getId(), e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ Error in sendPendingNotifications", e);
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

