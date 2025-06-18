package com.example.cleopatra.repository;

import com.example.cleopatra.model.UserOnlineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserOnlineStatusRepository extends JpaRepository<UserOnlineStatus, Long> {

    // ====================== ОСНОВНЫЕ МЕТОДЫ ======================

    /**
     * Найти статус пользователя по ID
     */
    Optional<UserOnlineStatus> findByUserId(Long userId);

    /**
     * Проверить существование записи по userId
     */
    boolean existsByUserId(Long userId);

    /**
     * Обновить онлайн статус и время последнего посещения
     */
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.isOnline = :isOnline, u.lastSeen = :lastSeen WHERE u.userId = :userId")
    int updateOnlineStatus(@Param("userId") Long userId,
                           @Param("isOnline") Boolean isOnline,
                           @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Обновить время последнего посещения
     */
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.lastSeen = :lastSeen WHERE u.userId = :userId")
    int updateLastSeen(@Param("userId") Long userId, @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Обновить информацию об устройстве
     */
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.deviceType = :deviceType, u.lastIp = :lastIp, u.userAgent = :userAgent WHERE u.userId = :userId")
    int updateDeviceInfo(@Param("userId") Long userId,
                         @Param("deviceType") String deviceType,
                         @Param("lastIp") String lastIp,
                         @Param("userAgent") String userAgent);

    /**
     * Подсчитать количество онлайн пользователей
     */
    @Query("SELECT COUNT(u) FROM UserOnlineStatus u WHERE u.isOnline = true")
    Long countOnlineUsers();

    /**
     * Отметить неактивных пользователей как офлайн
     */
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.isOnline = false WHERE u.isOnline = true AND u.lastSeen < :cutoffTime")
    int markInactiveUsersOffline(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Получить статус пользователей по списку ID
     */
    @Query("SELECT u FROM UserOnlineStatus u WHERE u.userId IN :userIds")
    List<UserOnlineStatus> findByUserIds(@Param("userIds") List<Long> userIds);

    // ====================== МЕТОДЫ ДЛЯ ШЕДУЛЕРА ======================

    /**
     * Подсчитать недавно активных пользователей
     */
    @Query("SELECT COUNT(u) FROM UserOnlineStatus u WHERE u.isOnline = true OR u.lastSeen > :cutoffTime")
    Long countRecentlyActiveUsers(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Удалить старые записи статуса
     */
    @Modifying
    @Query("DELETE FROM UserOnlineStatus u WHERE u.isOnline = false AND u.lastSeen < :cutoffTime")
    int deleteOldStatusRecords(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Исправить некорректные онлайн статусы
     */
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.isOnline = false WHERE u.isOnline = true AND u.lastSeen < :threshold")
    int fixInconsistentOnlineStatuses(@Param("threshold") LocalDateTime threshold);

    /**
     * Исправить записи с null значениями
     */
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.isOnline = false, u.lastSeen = COALESCE(u.lastSeen, u.createdAt, CURRENT_TIMESTAMP) WHERE u.isOnline IS NULL OR u.lastSeen IS NULL")
    int fixNullStatuses();

    /**
     * Принудительно отметить всех пользователей как офлайн
     */
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.isOnline = false WHERE u.isOnline = true")
    int forceAllUsersOffline(@Param("timestamp") LocalDateTime timestamp);

    /**
     * Получить статистику по типам устройств
     */
    @Query("SELECT u.deviceType as deviceType, COUNT(u) as count FROM UserOnlineStatus u WHERE u.isOnline = true GROUP BY u.deviceType ORDER BY COUNT(u) DESC")
    List<Object[]> getDeviceTypeStatistics();

    // ====================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ======================

    /**
     * Удалить статус пользователя по ID
     */
    @Modifying
    @Query("DELETE FROM UserOnlineStatus u WHERE u.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Принудительное обновление статуса
     */
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.isOnline = :isOnline, u.lastSeen = :lastSeen WHERE u.userId = :userId")
    int forceUpdateOnlineStatus(@Param("userId") Long userId,
                                @Param("isOnline") Boolean isOnline,
                                @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Подсчитать количество записей для пользователя
     */
    @Query("SELECT COUNT(u) FROM UserOnlineStatus u WHERE u.userId = :userId")
    int countByUserId(@Param("userId") Long userId);

    /**
     * Удалить ВСЕ записи для пользователя
     */
    @Modifying
    @Query(value = "DELETE FROM user_online_status WHERE user_id = :userId", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") Long userId);

    // ====================== МЕТОДЫ ДЛЯ MYSQL ======================

    /**
     * UPSERT для MySQL
     */
    @Modifying
    @Query(value = """
        INSERT INTO user_online_status (user_id, is_online, last_seen, device_type, created_at, updated_at) 
        VALUES (:userId, :isOnline, :lastSeen, :deviceType, :createdAt, :updatedAt)
        ON DUPLICATE KEY UPDATE 
            is_online = VALUES(is_online), 
            last_seen = VALUES(last_seen), 
            updated_at = VALUES(updated_at)
        """, nativeQuery = true)
    int upsertOnlineStatus(@Param("userId") Long userId,
                           @Param("isOnline") Boolean isOnline,
                           @Param("lastSeen") LocalDateTime lastSeen,
                           @Param("deviceType") String deviceType,
                           @Param("createdAt") LocalDateTime createdAt,
                           @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * INSERT IGNORE для MySQL
     */
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO user_online_status (user_id, is_online, last_seen, device_type, created_at, updated_at) 
        VALUES (:userId, :isOnline, :lastSeen, :deviceType, :createdAt, :updatedAt)
        """, nativeQuery = true)
    int createOrIgnoreStatus(@Param("userId") Long userId,
                             @Param("isOnline") Boolean isOnline,
                             @Param("lastSeen") LocalDateTime lastSeen,
                             @Param("deviceType") String deviceType,
                             @Param("createdAt") LocalDateTime createdAt,
                             @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Чистый SQL INSERT для MySQL
     */
    @Modifying
    @Query(value = """
        INSERT INTO user_online_status (user_id, is_online, last_seen, device_type, created_at, updated_at) 
        VALUES (:userId, :isOnline, :lastSeen, :deviceType, :createdAt, :updatedAt)
        """, nativeQuery = true)
    int insertNewStatus(@Param("userId") Long userId,
                        @Param("isOnline") Boolean isOnline,
                        @Param("lastSeen") LocalDateTime lastSeen,
                        @Param("deviceType") String deviceType,
                        @Param("createdAt") LocalDateTime createdAt,
                        @Param("updatedAt") LocalDateTime updatedAt);

    // ====================== МЕТОДЫ ДЛЯ POSTGRESQL ======================

    /**
     * UPSERT для PostgreSQL
     */
    @Modifying
    @Query(value = """
        INSERT INTO user_online_status (user_id, is_online, last_seen, device_type, created_at, updated_at) 
        VALUES (:userId, :isOnline, :lastSeen, :deviceType, :createdAt, :updatedAt)
        ON CONFLICT (user_id) DO UPDATE SET 
            is_online = EXCLUDED.is_online, 
            last_seen = EXCLUDED.last_seen, 
            updated_at = EXCLUDED.updated_at
        """, nativeQuery = true)
    int upsertOnlineStatusPostgres(@Param("userId") Long userId,
                                   @Param("isOnline") Boolean isOnline,
                                   @Param("lastSeen") LocalDateTime lastSeen,
                                   @Param("deviceType") String deviceType,
                                   @Param("createdAt") LocalDateTime createdAt,
                                   @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * INSERT ON CONFLICT DO NOTHING для PostgreSQL
     */
    @Modifying
    @Query(value = """
        INSERT INTO user_online_status (user_id, is_online, last_seen, device_type, created_at, updated_at) 
        VALUES (:userId, :isOnline, :lastSeen, :deviceType, :createdAt, :updatedAt)
        ON CONFLICT (user_id) DO NOTHING
        """, nativeQuery = true)
    int createOrIgnoreStatusPostgres(@Param("userId") Long userId,
                                     @Param("isOnline") Boolean isOnline,
                                     @Param("lastSeen") LocalDateTime lastSeen,
                                     @Param("deviceType") String deviceType,
                                     @Param("createdAt") LocalDateTime createdAt,
                                     @Param("updatedAt") LocalDateTime updatedAt);

    // ====================== МЕТОДЫ ДЛЯ H2 DATABASE ======================

    /**
     * MERGE для H2
     */
    @Modifying
    @Query(value = """
        MERGE INTO user_online_status (user_id, is_online, last_seen, device_type, created_at, updated_at) 
        VALUES (:userId, :isOnline, :lastSeen, :deviceType, :createdAt, :updatedAt)
        """, nativeQuery = true)
    int upsertOnlineStatusH2(@Param("userId") Long userId,
                             @Param("isOnline") Boolean isOnline,
                             @Param("lastSeen") LocalDateTime lastSeen,
                             @Param("deviceType") String deviceType,
                             @Param("createdAt") LocalDateTime createdAt,
                             @Param("updatedAt") LocalDateTime updatedAt);

    // ====================== ДОПОЛНИТЕЛЬНЫЕ УДОБНЫЕ МЕТОДЫ ======================

    /**
     * Найти всех онлайн пользователей
     */
    @Query("SELECT u FROM UserOnlineStatus u WHERE u.isOnline = true ORDER BY u.lastSeen DESC")
    List<UserOnlineStatus> findAllOnlineUsers();

    /**
     * Проверить, был ли пользователь онлайн недавно
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserOnlineStatus u WHERE u.userId = :userId AND (u.isOnline = true OR u.lastSeen > :recentTime)")
    boolean wasRecentlyOnline(@Param("userId") Long userId, @Param("recentTime") LocalDateTime recentTime);

    /**
     * Получить топ активных пользователей за период
     */
    @Query("SELECT u FROM UserOnlineStatus u WHERE u.lastSeen > :since ORDER BY u.lastSeen DESC")
    List<UserOnlineStatus> findTopActiveUsers(@Param("since") LocalDateTime since);

    /**
     * Найти пользователей онлайн по типу устройства
     */
    @Query("SELECT u FROM UserOnlineStatus u WHERE u.isOnline = true AND u.deviceType = :deviceType ORDER BY u.lastSeen DESC")
    List<UserOnlineStatus> findOnlineUsersByDeviceType(@Param("deviceType") String deviceType);

    /**
     * Подсчитать пользователей по типу устройства
     */
    @Query("SELECT COUNT(u) FROM UserOnlineStatus u WHERE u.isOnline = true AND u.deviceType = :deviceType")
    Long countOnlineUsersByDeviceType(@Param("deviceType") String deviceType);

    /**
     * Найти пользователей, которые были онлайн между датами
     */
    @Query("SELECT u FROM UserOnlineStatus u WHERE u.lastSeen BETWEEN :startDate AND :endDate ORDER BY u.lastSeen DESC")
    List<UserOnlineStatus> findUsersOnlineBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
}