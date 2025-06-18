package com.example.cleopatra.config;
import com.example.cleopatra.Scheduling.UserOnlineStatusScheduler;
import com.example.cleopatra.service.UserOnlineStatusService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;



/**
 * Обработчик событий запуска и остановки приложения
 * для корректного управления онлайн статусом пользователей
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupHandler {

    private final UserOnlineStatusService userOnlineStatusService;

    /**
     * Выполняется после полного запуска приложения
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("🚀 Приложение запущено. Инициализация онлайн статусов пользователей...");

            // Логируем конфигурацию шедулера
            UserOnlineStatusScheduler.logSchedulerConfiguration();

            // Отмечаем всех пользователей как офлайн при старте сервера
            // (так как сессии могли быть потеряны при перезапуске)
            int offlineCount = userOnlineStatusService.forceAllUsersOffline();

            if (offlineCount > 0) {
                log.info("✅ {} пользователей отмечены как офлайн при запуске сервера", offlineCount);
            } else {
                log.info("✅ Онлайн статусы пользователей инициализированы");
            }

            // Проверяем и исправляем некорректные данные
            int fixedCount = userOnlineStatusService.fixInconsistentStatuses();
            if (fixedCount > 0) {
                log.info("🔧 Исправлено {} некорректных записей статуса при запуске", fixedCount);
            }

            log.info("🎯 Система онлайн статусов готова к работе");

        } catch (Exception e) {
            log.error("❌ Ошибка при инициализации онлайн статусов: {}", e.getMessage(), e);
        }
    }

    /**
     * Выполняется при закрытии контекста приложения
     */
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed(ContextClosedEvent event) {
        try {
            log.info("🛑 Закрытие контекста приложения. Сохранение состояния онлайн статусов...");

            // Отмечаем всех пользователей как офлайн при остановке
            int offlineCount = userOnlineStatusService.forceAllUsersOffline();

            if (offlineCount > 0) {
                log.info("💾 {} пользователей отмечены как офлайн при остановке сервера", offlineCount);
            }

            log.info("✅ Состояние онлайн статусов сохранено");

        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении онлайн статусов: {}", e.getMessage(), e);
        }
    }

//    /**
//     * Альтернативный способ - через @PreDestroy
//     * Выполняется при уничтожении бина (graceful shutdown)
//     */
//    @PreDestroy
//    public void onDestroy() {
//        try {
//            log.info("🔄 Выполнение @PreDestroy. Финальное сохранение онлайн статусов...");
//
//            // Дополнительная проверка и сохранение состояния
//            userOnlineStatusService.forceAllUsersOffline();
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка в @PreDestroy при сохранении онлайн статусов: {}", e.getMessage(), e);
//        }
//    }
}