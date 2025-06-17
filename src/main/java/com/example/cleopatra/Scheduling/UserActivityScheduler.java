package com.example.cleopatra.Scheduling;

import com.example.cleopatra.config.NotificationWebSocketHandler;
import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.maper.NotificationMapper;
import com.example.cleopatra.model.Notification;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.NotificationRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.StoryService;
import com.example.cleopatra.service.impl.EmailQueueManager;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserActivityScheduler {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationMapper notificationMapper;
    private final StoryService storyService;

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

    }



        private static final int BATCH_SIZE = 100; // Размер пачки
        private static final int MAX_RETRIES = 3;

        /**
         * Отправляет накопившиеся уведомления батчами
         */
        @Scheduled(fixedRate = 60000) // Каждую минуту
        public void sendPendingNotificationsBatch() {
            log.debug("🔄 Starting batch notification processing...");

            try {
                int processedTotal = 0;
                int batchNumber = 1;

                List<Notification> batch;
                do {
                    // Получаем очередную пачку уведомлений
                    batch = notificationRepository.findPendingNotificationsForOnlineUsersWithLimit(BATCH_SIZE);

                    if (!batch.isEmpty()) {

                        int processedInBatch = processBatch(batch);
                        processedTotal += processedInBatch;

                        log.info("✅ Batch {} completed: {}/{} notifications sent",
                                batchNumber, processedInBatch, batch.size());
                        batchNumber++;
                    }

                } while (batch.size() == BATCH_SIZE); // Продолжаем пока пачка полная

                if (processedTotal > 0) {
                    log.info("🎉 Batch processing completed: {} total notifications processed", processedTotal);
                }

            } catch (Exception e) {
                log.error("❌ Error in batch notification processing", e);
            }
        }

        /**
         * Обрабатывает одну пачку уведомлений
         */
        private int processBatch(List<Notification> notifications) {
            // Группируем уведомления по получателям для оптимизации WebSocket соединений
            Map<Long, List<Notification>> notificationsByRecipient = notifications.stream()
                    .collect(Collectors.groupingBy(n -> n.getRecipient().getId()));

            List<Long> successfullyProcessedIds = new ArrayList<>();
            int sentCount = 0;

            for (Map.Entry<Long, List<Notification>> entry : notificationsByRecipient.entrySet()) {
                Long recipientId = entry.getKey();
                List<Notification> userNotifications = entry.getValue();

                try {
                    if (notificationWebSocketHandler.isUserConnected(recipientId)) {
                        // Отправляем все уведомления пользователю одной пачкой
                        List<NotificationDto> dtos = userNotifications.stream()
                                .map(notificationMapper::toWebSocketDto)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        if (!dtos.isEmpty()) {
                            boolean allSent = sendNotificationBatchToUser(recipientId, dtos);

                            if (allSent) {
                                // Добавляем ID успешно отправленных уведомлений
                                userNotifications.stream()
                                        .map(Notification::getId)
                                        .forEach(successfullyProcessedIds::add);
                                sentCount += userNotifications.size();

                                log.debug("📤 Sent {} notifications to user {}", userNotifications.size(), recipientId);
                            }
                        }
                    } else {
                        log.debug("👤 User {} is not connected, skipping {} notifications",
                                recipientId, userNotifications.size());
                    }

                } catch (Exception e) {
                    log.error("❌ Error processing notifications for user {}: {}", recipientId, e.getMessage());
                }
            }

            // Batch-обновление статуса отправленных уведомлений
            if (!successfullyProcessedIds.isEmpty()) {
                updateNotificationsAsSentBatch(successfullyProcessedIds);
            }

            return sentCount;
        }

        /**
         * Отправляет пачку уведомлений одному пользователю
         */
        private boolean sendNotificationBatchToUser(Long recipientId, List<NotificationDto> notifications) {
            try {
                // Можно отправить либо по одному, либо одним сообщением с массивом

                // Вариант 1: По одному (текущий подход)
                boolean allSent = true;
                for (NotificationDto dto : notifications) {
                    boolean sent = notificationWebSocketHandler.sendNotificationToUser(recipientId, dto);
                    if (!sent) {
                        allSent = false;
                        break;
                    }
                }
                return allSent;

                // Вариант 2: Одним сообщением (если ваш WebSocket поддерживает)
//                 return notificationWebSocketHandler.sendNotificationBatchToUser(recipientId, notifications);

            } catch (Exception e) {
                log.error("❌ Error sending notification batch to user {}", recipientId, e);
                return false;
            }
        }

    /**
     * Batch-обновление статуса уведомлений как отправленных
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Новая транзакция
    public void updateNotificationsAsSentBatch(List<Long> notificationIds) {
        if (notificationIds.isEmpty()) {
            return;
        }

        try {
            LocalDateTime sentAt = LocalDateTime.now();

            // Batch-обновление одним запросом
            int updated = notificationRepository.updateNotificationsAsSent(notificationIds, sentAt);

            log.debug("✅ Marked {} notifications as sent in batch", updated);

        } catch (Exception e) {
            log.error("❌ Error updating notifications as sent in batch", e);

            // Fallback: обновляем по одному
            fallbackUpdateNotifications(notificationIds);
        }
    }

    /**
     * Резервное обновление по одному (если batch не сработал)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Новая транзакция
    public void fallbackUpdateNotifications(List<Long> notificationIds) {
        LocalDateTime sentAt = LocalDateTime.now();

        for (Long id : notificationIds) {
            try {
                notificationRepository.findById(id).ifPresent(notification -> {
                    notification.setIsSent(true);
                    notification.setSentAt(sentAt);
                    notificationRepository.save(notification);
                });
            } catch (Exception e) {
                log.error("❌ Error updating notification {} in fallback", id, e);
            }
        }

        log.warn("⚠️ Used fallback update for {} notifications", notificationIds.size());
    }


        /**
         * Получение статистики необработанных уведомлений
         */
        @Transactional(readOnly = true)
        public Map<String, Object> getPendingNotificationsStats() {
            try {
                long totalPending = notificationRepository.countPendingNotifications();
                long pendingForOnlineUsers = notificationRepository.countPendingNotificationsForOnlineUsers();

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalPending", totalPending);
                stats.put("pendingForOnlineUsers", pendingForOnlineUsers);
                stats.put("batchSize", BATCH_SIZE);
                stats.put("estimatedBatches", (pendingForOnlineUsers + BATCH_SIZE - 1) / BATCH_SIZE);

                return stats;

            } catch (Exception e) {
                log.error("❌ Error getting pending notifications stats", e);
                return Map.of("error", e.getMessage());
            }
        }

    }


