package com.example.cleopatra.Scheduling;

import com.example.cleopatra.repository.StoryRepository;
import com.example.cleopatra.repository.StoryViewRepository;
import com.example.cleopatra.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoryScheduler {

    private final StoryService storyService;
    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;

    /**
     * Удаляет истекшие истории каждые 30 минут
     * Использует batch обработку по 100 записей для лучшей производительности
     * Cron: каждые 30 минут (0 секунд, 0 и 30 минут, каждый час)
     */
    @Scheduled(cron = "0 0,30 * * * *")
    public void deleteExpiredStories() {
        log.info("🗑️ Запуск задачи удаления истекших историй...");

        try {
            int totalDeleted = 0;
            int batchSize = 100;
            boolean hasMore = true;

            while (hasMore) {
                int deletedInBatch = storyService.deleteExpiredStoriesBatch(batchSize);
                totalDeleted += deletedInBatch;

                if (deletedInBatch < batchSize) {
                    // Если удалили меньше чем размер batch - больше нет записей
                    hasMore = false;
                } else {
                    // Небольшая пауза между batch'ами для снижения нагрузки на БД
                    Thread.sleep(100); // 100ms пауза
                }

                log.debug("📦 Batch обработан: удалено {} историй", deletedInBatch);
            }

            if (totalDeleted > 0) {
                log.info("✅ Общий итог: удалено {} истекших историй за {} batch'ей",
                        totalDeleted, (totalDeleted + batchSize - 1) / batchSize);
            } else {
                log.debug("📭 Истекших историй не найдено");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Задача удаления прервана", e);
        } catch (Exception e) {
            log.error("❌ Ошибка при удалении истекших историй", e);
        }
    }

    /**
     * Дополнительная задача - очистка каждую ночь в 02:00
     * Для более тщательной очистки и освобождения места в БД
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void nightlyCleanup() {
        log.info("🌙 Запуск ночной очистки историй...");

        try {
            int deletedCount = storyService.deleteExpiredStories();
            log.info("🌙 Ночная очистка: удалено {} историй", deletedCount);

            // Здесь можно добавить дополнительную логику:
            // - VACUUM для PostgreSQL
            // - Очистка временных файлов
            // - Статистика использования

        } catch (Exception e) {
            log.error("❌ Ошибка при ночной очистке", e);
        }
    }


    @Scheduled(fixedRate = 7200000)
//@Scheduled(fixedRate = 120000) // 2 минуты = 120000 мс
    public void logStoriesStatistics() {
        try {
            long activeStoriesCount = storyRepository.count();
            log.info("📊 Активных историй: {}", activeStoriesCount);
        } catch (Exception e) {
            log.error("❌ Ошибка при мониторинге историй", e);
        }
    }
}