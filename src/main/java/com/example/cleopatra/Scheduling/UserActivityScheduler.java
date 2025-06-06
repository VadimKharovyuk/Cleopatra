package com.example.cleopatra.Scheduling;

import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
@Component
@Slf4j
@RequiredArgsConstructor
public class UserActivityScheduler {
    private final UserRepository userRepository;

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
}

