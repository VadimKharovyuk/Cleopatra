package com.example.cleopatra.service;

import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnlineStatusCleanupTask {

    private final UserRepository userRepository;

    /**
     * Каждые 10 минут очищаем пользователей без активности более 15 минут
     */
    @Scheduled(fixedRate = 600000) // 10 минут
    @Transactional
    public void cleanupStaleOnlineUsers() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(15); // 15 минут без активности

            int updatedCount = userRepository.setOfflineForInactiveUsers(threshold);

            if (updatedCount > 0) {
                log.info("🧹 Cleaned up {} stale online users", updatedCount);
            }

        } catch (Exception e) {
            log.error("❌ Error cleaning up stale online users", e);
        }
    }
}
