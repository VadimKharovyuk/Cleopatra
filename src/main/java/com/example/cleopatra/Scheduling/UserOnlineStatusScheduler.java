package com.example.cleopatra.Scheduling;

import com.example.cleopatra.service.UserOnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ОПТИМИЗИРОВАННЫЙ шедулер для работы совместно с JavaScript
 * Фокус на очистке, статистике и резервной защите
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        value = "app.user-online.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class UserOnlineStatusScheduler {

    private final UserOnlineStatusService userOnlineStatusService;

    // ====================== ОПТИМИЗИРОВАННЫЕ КОНСТАНТЫ ======================

    /**
     * УВЕЛИЧЕННЫЙ таймаут - JavaScript обновляет каждые 15 сек,
     * поэтому даем больше времени перед пометкой как офлайн
     */
    private static final int INACTIVE_TIMEOUT_MINUTES = 5; // Было 15, стало 5 (более агрессивно)

    /**
     * Очистка старых записей (без изменений)
     */
    private static final int CLEANUP_AFTER_DAYS = 30;

    /**
     * УВЕЛИЧЕННЫЙ интервал проверки неактивности - JavaScript делает основную работу
     * 30 минут вместо 10 (резервная проверка)
     */
    private static final long INACTIVE_CHECK_INTERVAL = 30 * 60 * 1000L; // Было 10 мин, стало 30

    /**
     * Статистика каждые 15 минут (чаще для мониторинга)
     */
    private static final long STATISTICS_INTERVAL = 15 * 60 * 1000L; // Было 30 мин, стало 15

    /**
     * Проверка консистентности каждые 2 часа (чаще для контроля)
     */
    private static final long CONSISTENCY_CHECK_INTERVAL = 2 * 60 * 60 * 1000L; // Было 6 часов, стало 2

    // ====================== ОСНОВНЫЕ ЗАДАЧИ ======================

    /**
     * РЕЗЕРВНАЯ проверка неактивных пользователей
     * Теперь выполняется реже (каждые 30 минут) - JavaScript делает основную работу
     */
    @Scheduled(fixedRate = INACTIVE_CHECK_INTERVAL)
    public void markInactiveUsersOfflineBackup() {
        try {
            log.debug("🔄 Резервная проверка неактивных пользователей (шедулер)...");

            long startTime = System.currentTimeMillis();
            int updatedCount = userOnlineStatusService.markInactiveUsersOffline(INACTIVE_TIMEOUT_MINUTES);
            long duration = System.currentTimeMillis() - startTime;

            if (updatedCount > 0) {
                log.warn("⚠️ Шедулер нашел {} пользователей, которые не были отмечены JS как офлайн за {} мс",
                        updatedCount, duration);
                log.warn("💡 Возможно, проблемы с JavaScript или пользователи резко закрыли браузер");
            } else {
                log.debug("✅ JavaScript корректно обработал все статусы. Время выполнения: {} мс", duration);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка в резервной проверке неактивных пользователей: {}", e.getMessage(), e);
        }
    }

    /**
     * Очистка старых записей (без изменений - важная задача)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldStatusRecords() {
        try {
            log.info("🗑️ Запуск очистки старых записей статуса пользователей...");

            long startTime = System.currentTimeMillis();
            int deletedCount = userOnlineStatusService.cleanupOldStatusRecords(CLEANUP_AFTER_DAYS);
            long duration = System.currentTimeMillis() - startTime;

            if (deletedCount > 0) {
                log.info("✅ Удалено {} старых записей статуса за {} мс", deletedCount, duration);
            } else {
                log.info("✅ Старых записей для удаления не найдено. Время выполнения: {} мс", duration);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при очистке старых записей статуса: {}", e.getMessage(), e);
        }
    }

    /**
     * УЛУЧШЕННАЯ статистика - теперь чаще для мониторинга эффективности JS
     */
    @Scheduled(fixedRate = STATISTICS_INTERVAL)
    public void logOnlineStatistics() {
        try {
            Long onlineCount = userOnlineStatusService.getOnlineUsersCount();
            Long recentlyActiveCount = userOnlineStatusService.getRecentlyActiveUsersCount(60);

            log.info("📊 Онлайн статистика: {} онлайн, {} активны за час", onlineCount, recentlyActiveCount);

            // Дополнительная проверка эффективности JS
            Long veryRecentlyActive = userOnlineStatusService.getRecentlyActiveUsersCount(2); // за 2 минуты
            if (onlineCount > 0 && veryRecentlyActive != null) {
                double jsEfficiency = (veryRecentlyActive.doubleValue() / onlineCount.doubleValue()) * 100;
                log.debug("⚡ Эффективность JS: {:.1f}% пользователей активны за последние 2 мин", jsEfficiency);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при получении статистики: {}", e.getMessage(), e);
        }
    }

    /**
     * УСИЛЕННАЯ проверка консистентности (чаще, т.к. больше активности)
     */
    @Scheduled(fixedRate = CONSISTENCY_CHECK_INTERVAL)
    public void checkDataConsistency() {
        try {
            log.debug("🔍 Проверка консистентности данных статуса...");

            long startTime = System.currentTimeMillis();
            int fixedCount = userOnlineStatusService.fixInconsistentStatuses();
            long duration = System.currentTimeMillis() - startTime;

            if (fixedCount > 0) {
                log.warn("⚠️ Исправлено {} некорректных записей статуса за {} мс", fixedCount, duration);
                log.warn("💡 Рекомендуется проверить работу JavaScript или нагрузку на сервер");
            } else {
                log.debug("✅ Данные консистентны. Время выполнения: {} мс", duration);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при проверке консистентности: {}", e.getMessage(), e);
        }
    }

    /**
     * НОВАЯ ЗАДАЧА: Мониторинг эффективности JavaScript
     * Каждые 5 минут проверяем, работает ли JS корректно
     */
    @Scheduled(fixedRate = 5 * 60 * 1000L) // Каждые 5 минут
    public void monitorJavaScriptEffectiveness() {
        try {
            Long onlineCount = userOnlineStatusService.getOnlineUsersCount();

            if (onlineCount > 0) {
                // Проверяем, сколько пользователей были активны за последние 30 секунд
                Long recentlyActive = userOnlineStatusService.getRecentlyActiveUsersCount(1); // за последнюю минуту

                if (recentlyActive != null && onlineCount > 5) { // Если больше 5 пользователей онлайн
                    double activityRatio = (recentlyActive.doubleValue() / onlineCount.doubleValue()) * 100;

                    if (activityRatio < 20) { // Если менее 20% пользователей активны
                        log.warn("⚠️ Низкая активность JavaScript: только {:.1f}% онлайн пользователей активны", activityRatio);
                        log.warn("💡 Возможные причины: проблемы с JS, высокая нагрузка, сетевые проблемы");
                    } else {
                        log.debug("✅ JavaScript работает эффективно: {:.1f}% пользователей активны", activityRatio);
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при мониторинге эффективности JavaScript: {}", e.getMessage(), e);
        }
    }

    /**
     * Еженедельная детальная статистика (улучшенная)
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void weeklyStatisticsReport() {
        try {
            log.info("📈 Еженедельный отчет по активности пользователей...");

            Long totalOnline = userOnlineStatusService.getOnlineUsersCount();
            Long activeLastDay = userOnlineStatusService.getRecentlyActiveUsersCount(24 * 60);
            Long activeLastWeek = userOnlineStatusService.getRecentlyActiveUsersCount(7 * 24 * 60);

            log.info("📊 Еженедельная статистика:");
            log.info("  • Сейчас онлайн: {} пользователей", totalOnline);
            log.info("  • Активны за 24 часа: {} пользователей", activeLastDay);
            log.info("  • Активны за неделю: {} пользователей", activeLastWeek);

            // Оценка эффективности системы
            if (activeLastDay != null && activeLastWeek != null && activeLastWeek > 0) {
                double dailyRetention = (activeLastDay.doubleValue() / activeLastWeek.doubleValue()) * 100;
                log.info("  • Дневная активность: {:.1f}% от недельной", dailyRetention);
            }

            // Статистика по устройствам
            var deviceStats = userOnlineStatusService.getDeviceTypeStatistics();
            if (!deviceStats.isEmpty()) {
                log.info("  • Распределение по устройствам:");
                deviceStats.forEach(stat -> {
                    Object[] data = (Object[]) stat;
                    log.info("    - {}: {} пользователей", data[0], data[1]);
                });
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при генерации еженедельного отчета: {}", e.getMessage(), e);
        }
    }

    // ====================== УТИЛИТЫ ======================

    /**
     * Логирование конфигурации (обновленное)
     */
    public static void logSchedulerConfiguration() {
        log.info("🔧 Конфигурация ОПТИМИЗИРОВАННОГО шедулера:");
        log.info("  • ⚡ JavaScript обновляет статус каждые 15 сек");
        log.info("  • 🔄 Резервная проверка неактивности: {} минут", INACTIVE_TIMEOUT_MINUTES);
        log.info("  • 🗑️ Очистка записей старше: {} дней", CLEANUP_AFTER_DAYS);
        log.info("  • ⏰ Интервал резервной проверки: {} минут", INACTIVE_CHECK_INTERVAL / 60000);
        log.info("  • 📊 Интервал статистики: {} минут", STATISTICS_INTERVAL / 60000);
        log.info("  • 🔍 Интервал проверки консистентности: {} часов", CONSISTENCY_CHECK_INTERVAL / 3600000);
        log.info("  • 📡 Мониторинг JS: каждые 5 минут");
    }
}