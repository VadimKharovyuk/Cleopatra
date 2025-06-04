package com.example.cleopatra.service;

import com.example.cleopatra.model.User;
import com.example.cleopatra.model.UserOnlineStatus;
import com.example.cleopatra.repository.UserOnlineStatusRepository;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserOnlineStatusService {


    private final UserOnlineStatusRepository onlineStatusRepository;
    private final UserRepository userRepository;

    /**
     * Обновить онлайн статус пользователя
     */

    @Transactional
    public void updateOnlineStatus(Long userId, boolean isOnline) {
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
                        .deviceType("WEB") // Можно передавать как параметр
                        .build();

                onlineStatusRepository.save(status);
                log.info("Создан новый онлайн статус для пользователя {}: {}", userId, isOnline);
            } else {
                log.debug("Обновлен онлайн статус для пользователя {}: {}", userId, isOnline);
            }

        } catch (Exception e) {
            log.error("Ошибка при обновлении онлайн статуса для пользователя {}: {}", userId, e.getMessage());
            // Не пробрасываем исключение, чтобы не сломать основную логику
        }
    }

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
            log.error("Ошибка при обновлении онлайн статуса для пользователя {}: {}", userId, e.getMessage());
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
     * Проверить, был ли пользователь онлайн недавно
     */
    @Transactional(readOnly = true)
    public boolean wasRecentlyOnline(Long userId, int minutesAgo) {
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(minutesAgo);
        return onlineStatusRepository.wasRecentlyOnline(userId, recentTime);
    }

    /**
     * Получить список онлайн пользователей
     */
    @Transactional(readOnly = true)
    public List<UserOnlineStatus> getOnlineUsers() {
        return onlineStatusRepository.findAllOnlineUsers();
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

    // Вспомогательные методы для парсинга clientInfo

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

        // Ищем IP в формате "IP: xxx.xxx.xxx.xxx"
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

        // Ищем User-Agent в формате "UserAgent: ..."
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
