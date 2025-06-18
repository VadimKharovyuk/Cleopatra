package com.example.cleopatra.service;

import com.example.cleopatra.model.User;
import com.example.cleopatra.model.UserOnlineStatus;
import com.example.cleopatra.repository.UserOnlineStatusRepository;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserOnlineStatusService {

    private final UserOnlineStatusRepository onlineStatusRepository;
    private final UserRepository userRepository;

    // ====================== ДОБАВИТЬ ЭТИ МЕТОДЫ ======================

    /**
     * Простое обновление онлайн статуса БЕЗ clientInfo
     */
    @Transactional
    public void updateOnlineStatus(Long userId, boolean isOnline) {
        updateOnlineStatus(userId, isOnline, null); // Вызываем существующий метод
    }



    /**
     * ИСПРАВЛЕННОЕ ОКОНЧАТЕЛЬНОЕ РЕШЕНИЕ - только SQL запросы
     */
    @Transactional
    public void updateOnlineStatusFinal(Long userId, boolean isOnline) {
        log.info("🔄 ФИНАЛЬНОЕ обновление: userId={}, isOnline={}", userId, isOnline);

        LocalDateTime now = LocalDateTime.now(); // Объявляем здесь для доступности везде

        try {
            // 1. Попытка обновления через SQL
            int updated = onlineStatusRepository.updateOnlineStatus(userId, isOnline, now);
            log.debug("📊 SQL UPDATE: обновлено {} строк", updated);

            if (updated > 0) {
                log.info("✅ УСПЕШНО обновлен через SQL: userId={}, isOnline={}", userId, isOnline);
                return;
            }

            // 2. Если не обновилось, используем UPSERT через native SQL
            log.debug("➕ Попытка UPSERT через native SQL...");

            int result = onlineStatusRepository.upsertOnlineStatus(
                    userId, isOnline, now, "WEB", now, now
            );

            if (result > 0) {
                log.info("✅ УСПЕШНО создан/обновлен через UPSERT: userId={}", userId);
            } else {
                log.error("❌ UPSERT не сработал для userId={}", userId);
            }

        } catch (Exception e) {
            log.error("❌ КРИТИЧЕСКАЯ ошибка финального обновления для userId={}: {}", userId, e.getMessage());
            log.error("📊 Стек-трейс финального метода:", e);

            // Последняя попытка - прямой SQL
            try {
                log.debug("🆘 ПОСЛЕДНЯЯ ПОПЫТКА - прямое создание...");
                onlineStatusRepository.createOrIgnoreStatus(userId, isOnline, now, "WEB", now, now);
                log.info("🆘 Последняя попытка завершена для userId={}", userId);
            } catch (Exception e2) {
                log.error("💀 ВСЕ ПОПЫТКИ НЕУДАЧНЫ для userId={}: {}", userId, e2.getMessage());
            }
        }
    }

    /**
     * Упрощенная версия - только основные операции
     */
    @Transactional
    public void updateOnlineStatusUltraSimple(Long userId, boolean isOnline) {
        log.info("🔄 УЛЬТРА-ПРОСТОЕ обновление: userId={}, isOnline={}", userId, isOnline);

        LocalDateTime now = LocalDateTime.now();

        try {
            // Попытка 1: Обычный UPDATE
            int updated = onlineStatusRepository.updateOnlineStatus(userId, isOnline, now);

            if (updated > 0) {
                log.info("✅ Обновлен: userId={}", userId);
                return;
            }

            // Попытка 2: Прямой INSERT
            onlineStatusRepository.insertNewStatus(userId, isOnline, now, "WEB", now, now);
            log.info("✅ Создан: userId={}", userId);

        } catch (Exception e) {
            log.error("❌ Ошибка: {}", e.getMessage());

            // Последняя попытка: DELETE + INSERT
            try {
                onlineStatusRepository.deleteAllByUserId(userId);
                onlineStatusRepository.insertNewStatus(userId, isOnline, now, "WEB", now, now);
                log.info("✅ Пересоздан: userId={}", userId);
            } catch (Exception e2) {
                log.error("💀 Финальная ошибка: {}", e2.getMessage());
            }
        }
    }

    /**
     * Метод для полной очистки и пересоздания статуса
     */
    @Transactional
    public void recreateUserStatus(Long userId, boolean isOnline) {
        log.info("🔄 ПОЛНОЕ ПЕРЕСОЗДАНИЕ статуса: userId={}, isOnline={}", userId, isOnline);

        LocalDateTime now = LocalDateTime.now(); // Объявляем здесь

        try {
            // 1. Полная очистка всех записей для пользователя
            log.debug("🗑️ Удаление ВСЕХ записей статуса для userId={}", userId);
            onlineStatusRepository.deleteAllByUserId(userId);
            onlineStatusRepository.flush();

            // 2. Небольшая пауза
            Thread.sleep(100);

            // 3. Создание через чистый SQL INSERT
            log.debug("➕ Создание новой записи через чистый SQL...");

            int created = onlineStatusRepository.insertNewStatus(userId, isOnline, now, "WEB", now, now);

            if (created > 0) {
                log.info("✅ УСПЕШНО пересоздан статус: userId={}, isOnline={}", userId, isOnline);
            } else {
                log.error("❌ Не удалось создать новый статус для userId={}", userId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ Прервана пауза при пересоздании статуса");
        } catch (Exception e) {
            log.error("❌ Критическая ошибка пересоздания для userId={}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Простейший метод - только самое необходимое
     */
    @Transactional
    public void setUserOnline(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        try {
            // Просто устанавливаем пользователя онлайн
            int result = onlineStatusRepository.updateOnlineStatus(userId, true, now);

            if (result == 0) {
                // Если не обновилось, создаем новую запись
                onlineStatusRepository.insertNewStatus(userId, true, now, "WEB", now, now);
            }

            log.info("✅ Пользователь {} установлен как онлайн", userId);

        } catch (Exception e) {
            log.error("❌ Ошибка установки онлайн статуса: {}", e.getMessage());
        }
    }

    /**
     * Простейший метод для офлайн
     */
    @Transactional
    public void setUserOffline(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        try {
            // Просто устанавливаем пользователя офлайн
            int result = onlineStatusRepository.updateOnlineStatus(userId, false, now);

            if (result == 0) {
                // Если не обновилось, создаем новую запись
                onlineStatusRepository.insertNewStatus(userId, false, now, "WEB", now, now);
            }

            log.info("✅ Пользователь {} установлен как офлайн", userId);

        } catch (Exception e) {
            log.error("❌ Ошибка установки офлайн статуса: {}", e.getMessage());
        }
    }



    /**
     * Метод для проверки и диагностики состояния
     */
    @Transactional(readOnly = true)
    public void diagnoseUserStatus(Long userId) {
        log.info("🔍 ДИАГНОСТИКА статуса пользователя: userId={}", userId);

        try {
            // Проверяем существование пользователя
            boolean userExists = userRepository.existsById(userId);
            log.info("👤 Пользователь существует: {}", userExists);

            // Проверяем существование статуса
            boolean statusExists = onlineStatusRepository.existsByUserId(userId);
            log.info("📊 Статус существует: {}", statusExists);

            if (statusExists) {
                // Получаем текущее состояние
                Optional<UserOnlineStatus> current = onlineStatusRepository.findByUserId(userId);
                if (current.isPresent()) {
                    UserOnlineStatus status = current.get();
                    log.info("📋 ТЕКУЩЕЕ СОСТОЯНИЕ:");
                    log.info("  - userId: {}", status.getUserId());
                    log.info("  - isOnline: {}", status.getIsOnline());
                    log.info("  - lastSeen: {}", status.getLastSeen());
                    log.info("  - deviceType: {}", status.getDeviceType());
                    log.info("  - createdAt: {}", status.getCreatedAt());
                    log.info("  - updatedAt: {}", status.getUpdatedAt());
                }
            }

            // Проверяем количество записей для этого пользователя
            int count = onlineStatusRepository.countByUserId(userId);
            log.info("🔢 Количество записей статуса для userId={}: {}", userId, count);

            if (count > 1) {
                log.error("⚠️ НАЙДЕНО ДУБЛИРОВАНИЕ! У пользователя {} есть {} записей статуса", userId, count);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка диагностики для userId={}: {}", userId, e.getMessage(), e);
        }
    }



    /**
     * Метод с принудительной пересинхронизацией
     */
    @Transactional
    public void forceResyncUserStatus(Long userId, boolean isOnline) {
        log.info("🔄 ПРИНУДИТЕЛЬНАЯ пересинхронизация: userId={}, isOnline={}", userId, isOnline);

        try {
            // 1. Удаляем существующую запись
            log.debug("🗑️ Удаление существующей записи...");
            onlineStatusRepository.deleteByUserId(userId);
            onlineStatusRepository.flush(); // Принудительно выполняем DELETE

            // 2. Создаем новую
            log.debug("➕ Создание новой записи...");
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + userId));

            UserOnlineStatus newStatus = UserOnlineStatus.builder()
                    .userId(userId)
                    .user(user)
                    .isOnline(isOnline)
                    .lastSeen(LocalDateTime.now())
                    .deviceType("WEB")
                    .build();

            UserOnlineStatus saved = onlineStatusRepository.saveAndFlush(newStatus);
            log.info("✅ ПРИНУДИТЕЛЬНО создан статус: userId={}, isOnline={}", saved.getUserId(), saved.getIsOnline());

        } catch (Exception e) {
            log.error("❌ Ошибка принудительной пересинхронизации для userId={}: {}", userId, e.getMessage());
            log.error("📊 Стек-трейс:", e);
        }
    }

    /**
     * Метод обновления без транзакционных конфликтов
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOnlineStatusNewTransaction(Long userId, boolean isOnline) {
        log.info("🔄 НОВАЯ ТРАНЗАКЦИЯ обновление: userId={}, isOnline={}", userId, isOnline);

        try {
            LocalDateTime now = LocalDateTime.now();

            // Попытка прямого SQL обновления
            int updated = onlineStatusRepository.forceUpdateOnlineStatus(userId, isOnline, now);

            if (updated > 0) {
                log.info("✅ Обновлен в новой транзакции: userId={}", userId);
            } else {
                log.debug("❌ SQL не обновил, создаем новую запись...");

                // Проверяем, есть ли запись
                boolean exists = onlineStatusRepository.existsByUserId(userId);
                if (!exists) {
                    // Создаем новую
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + userId));

                    UserOnlineStatus newStatus = UserOnlineStatus.builder()
                            .userId(userId)
                            .user(user)
                            .isOnline(isOnline)
                            .lastSeen(now)
                            .deviceType("WEB")
                            .build();

                    onlineStatusRepository.save(newStatus);
                    log.info("✅ Создан в новой транзакции: userId={}", userId);
                } else {
                    log.warn("⚠️ Запись существует, но не обновляется. Возможна блокировка.");
                }
            }

        } catch (Exception e) {
            log.error("❌ Ошибка в новой транзакции для userId={}: {}", userId, e.getMessage());
            log.error("📊 Стек-трейс:", e);
        }
    }



    // ====================== ВАШ СУЩЕСТВУЮЩИЙ КОД ======================

    /**
     * Обновить онлайн статус с информацией о клиенте
     */
    @Transactional
    public void updateOnlineStatus(Long userId, boolean isOnline, String clientInfo) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Пытаемся обновить существующую запись
            int updatedRows = onlineStatusRepository.updateOnlineStatus(userId, isOnline, now);

            if (updatedRows == 0) {
                // Если записи нет, создаем новую
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + userId));

                UserOnlineStatus status = UserOnlineStatus.builder()
                        .userId(userId)
                        .user(user)
                        .isOnline(isOnline)
                        .lastSeen(now)
                        .deviceType(parseDeviceType(clientInfo))
                        .lastIp(parseIpFromClientInfo(clientInfo))
                        .userAgent(parseUserAgentFromClientInfo(clientInfo))
                        .build();

                onlineStatusRepository.save(status);
                log.info("Создан новый онлайн статус для пользователя {}: {}", userId, isOnline);
            } else {
                // Обновляем дополнительную информацию о клиенте
                if (clientInfo != null) {
                    onlineStatusRepository.updateDeviceInfo(
                            userId,
                            parseDeviceType(clientInfo),
                            parseIpFromClientInfo(clientInfo),
                            parseUserAgentFromClientInfo(clientInfo)
                    );
                }
                log.debug("Обновлен онлайн статус для пользователя {}: {}", userId, isOnline);
            }

        } catch (Exception e) {
            log.error("Ошибка при обновлении онлайн статуса для пользователя {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Обновить время последнего посещения
     */
    @Transactional
    public void updateLastSeen(Long userId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int updatedRows = onlineStatusRepository.updateLastSeen(userId, now);

            if (updatedRows > 0) {
                log.debug("Обновлено время последнего посещения для пользователя {}", userId);
            }
        } catch (Exception e) {
            log.error("Ошибка при обновлении времени последнего посещения для пользователя {}: {}",
                    userId, e.getMessage());
        }
    }

    /**
     * Проверить, онлайн ли пользователь
     */
    @Transactional(readOnly = true)
    public boolean isUserOnline(Long userId) {
        return onlineStatusRepository.findByUserId(userId)
                .map(status -> Boolean.TRUE.equals(status.getIsOnline()))
                .orElse(false);
    }

    /**
     * Получить время последнего посещения
     */
    @Transactional(readOnly = true)
    public LocalDateTime getLastSeen(Long userId) {
        return onlineStatusRepository.findByUserId(userId)
                .map(UserOnlineStatus::getLastSeen)
                .orElse(null);
    }

    /**
     * Получить количество онлайн пользователей
     */
    @Transactional(readOnly = true)
    public Long getOnlineUsersCount() {
        return onlineStatusRepository.countOnlineUsers();
    }

    /**
     * Отметить неактивных пользователей как офлайн
     */
    @Transactional
    public int markInactiveUsersOffline(int minutesInactive) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutesInactive);
        int updatedCount = onlineStatusRepository.markInactiveUsersOffline(cutoffTime);

        if (updatedCount > 0) {
            log.info("Отмечено {} неактивных пользователей как офлайн", updatedCount);
        }

        return updatedCount;
    }

    /**
     * Получить статус пользователей по их ID
     */
    @Transactional(readOnly = true)
    public List<UserOnlineStatus> getUsersOnlineStatus(List<Long> userIds) {
        return onlineStatusRepository.findByUserIds(userIds);
    }

    /**
     * Получить количество недавно активных пользователей
     */
    @Transactional(readOnly = true)
    public Long getRecentlyActiveUsersCount(int minutesAgo) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutesAgo);
        return onlineStatusRepository.countRecentlyActiveUsers(cutoffTime);
    }

    /**
     * Очистить старые записи статуса
     */
    @Transactional
    public int cleanupOldStatusRecords(int daysOld) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = onlineStatusRepository.deleteOldStatusRecords(cutoffTime);

        if (deletedCount > 0) {
            log.info("Удалено {} старых записей статуса (старше {} дней)", deletedCount, daysOld);
        }

        return deletedCount;
    }

    /**
     * Исправить некорректные записи статуса
     */
    @Transactional
    public int fixInconsistentStatuses() {
        int fixedCount = 0;

        try {
            LocalDateTime inconsistencyThreshold = LocalDateTime.now().minusHours(1);
            int inconsistentCount = onlineStatusRepository.fixInconsistentOnlineStatuses(inconsistencyThreshold);
            fixedCount += inconsistentCount;

            if (inconsistentCount > 0) {
                log.warn("Исправлено {} записей с некорректным онлайн статусом", inconsistentCount);
            }

            int nullFixCount = onlineStatusRepository.fixNullStatuses();
            fixedCount += nullFixCount;

            if (nullFixCount > 0) {
                log.warn("Исправлено {} записей с null значениями", nullFixCount);
            }

        } catch (Exception e) {
            log.error("Ошибка при исправлении некорректных статусов: {}", e.getMessage(), e);
        }

        return fixedCount;
    }

    /**
     * Принудительно отметить всех пользователей как офлайн
     */
    @Transactional
    public int forceAllUsersOffline() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int updatedCount = onlineStatusRepository.forceAllUsersOffline(now);

            if (updatedCount > 0) {
                log.info("Все пользователи ({}) отмечены как офлайн при перезапуске", updatedCount);
            }

            return updatedCount;

        } catch (Exception e) {
            log.error("Ошибка при переводе всех пользователей в офлайн: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Получить статистику по устройствам
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDeviceTypeStatistics() {
        return onlineStatusRepository.getDeviceTypeStatistics();
    }

    // ====================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ======================

    private String parseDeviceType(String clientInfo) {
        if (clientInfo == null) return "WEB";

        String lowerInfo = clientInfo.toLowerCase();
        if (lowerInfo.contains("mobile") || lowerInfo.contains("android") || lowerInfo.contains("iphone")) {
            return "MOBILE";
        } else if (lowerInfo.contains("desktop") || lowerInfo.contains("electron")) {
            return "DESKTOP";
        }
        return "WEB";
    }

    private String parseIpFromClientInfo(String clientInfo) {
        if (clientInfo == null) return null;

        if (clientInfo.contains("IP: ")) {
            String ip = clientInfo.substring(clientInfo.indexOf("IP: ") + 4);
            if (ip.contains(",")) {
                ip = ip.substring(0, ip.indexOf(","));
            }
            return ip.length() > 45 ? ip.substring(0, 45) : ip;
        }
        return null;
    }

    private String parseUserAgentFromClientInfo(String clientInfo) {
        if (clientInfo == null) return null;

        if (clientInfo.contains("UserAgent: ")) {
            String userAgent = clientInfo.substring(clientInfo.indexOf("UserAgent: ") + 11);
            if (userAgent.contains(",")) {
                userAgent = userAgent.substring(0, userAgent.indexOf(","));
            }
            return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
        }
        return null;
    }
}