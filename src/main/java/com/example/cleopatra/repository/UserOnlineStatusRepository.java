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

    Optional<UserOnlineStatus> findByUserId(Long userId);

    @Query("SELECT u FROM UserOnlineStatus u WHERE u.isOnline = true")
    List<UserOnlineStatus> findAllOnlineUsers();

    @Query("SELECT u FROM UserOnlineStatus u WHERE u.lastSeen > :since")
    List<UserOnlineStatus> findUsersOnlineSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM UserOnlineStatus u WHERE u.isOnline = true")
    Long countOnlineUsers();

    @Query("SELECT u FROM UserOnlineStatus u WHERE u.userId IN :userIds")
    List<UserOnlineStatus> findByUserIds(@Param("userIds") List<Long> userIds);

    // ✅ Добавляем недостающий метод updateOnlineStatus
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.isOnline = :isOnline, u.lastSeen = :lastSeen " +
            "WHERE u.userId = :userId")
    int updateOnlineStatus(@Param("userId") Long userId,
                           @Param("isOnline") boolean isOnline,
                           @Param("lastSeen") LocalDateTime lastSeen);

    // ✅ Дополнительные полезные методы
    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.isOnline = false WHERE u.isOnline = true AND u.lastSeen < :cutoffTime")
    int markInactiveUsersOffline(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.lastSeen = :lastSeen WHERE u.userId = :userId")
    int updateLastSeen(@Param("userId") Long userId, @Param("lastSeen") LocalDateTime lastSeen);

    @Modifying
    @Query("UPDATE UserOnlineStatus u SET u.deviceType = :deviceType, u.lastIp = :lastIp, " +
            "u.userAgent = :userAgent WHERE u.userId = :userId")
    int updateDeviceInfo(@Param("userId") Long userId,
                         @Param("deviceType") String deviceType,
                         @Param("lastIp") String lastIp,
                         @Param("userAgent") String userAgent);

    // Проверить, был ли пользователь онлайн недавно
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserOnlineStatus u " +
            "WHERE u.userId = :userId AND (u.isOnline = true OR u.lastSeen > :recentTime)")
    boolean wasRecentlyOnline(@Param("userId") Long userId, @Param("recentTime") LocalDateTime recentTime);

    // Получить пользователей, которые были онлайн в определенный период
    @Query("SELECT u FROM UserOnlineStatus u WHERE u.lastSeen BETWEEN :startTime AND :endTime " +
            "ORDER BY u.lastSeen DESC")
    List<UserOnlineStatus> findUsersOnlineBetween(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
}
