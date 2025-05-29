package com.example.cleopatra.repository;
import com.example.cleopatra.model.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {


    // Проверить существование активного устройства
    @Query("SELECT CASE WHEN COUNT(td) > 0 THEN true ELSE false END " +
            "FROM TrustedDevice td WHERE td.deviceId = :deviceId " +
            "AND td.user.id = :userId AND td.isActive = true")
    boolean existsByDeviceIdAndUserIdAndIsActive(
            @Param("deviceId") String deviceId,
            @Param("userId") Long userId
    );



    /**
     * Подсчитывает количество активных доверенных устройств пользователя
     */
    @Query("SELECT COUNT(t) FROM TrustedDevice t WHERE t.user.id = :userId AND t.isActive = :isActive")
    int countByUserIdAndIsActive(@Param("userId") Long userId, @Param("isActive") boolean isActive);

    /**
     * Находит устройство по deviceId и userId (включая неактивные)
     */
    @Query("SELECT t FROM TrustedDevice t WHERE t.deviceId = :deviceId AND t.user.id = :userId")
    Optional<TrustedDevice> findByDeviceIdAndUserId(@Param("deviceId") String deviceId, @Param("userId") Long userId);

    /**
     * Находит активные доверенные устройства пользователя
     */
    @Query("SELECT t FROM TrustedDevice t WHERE t.user.id = :userId AND t.isActive = :isActive ORDER BY t.lastUsedAt DESC")
    List<TrustedDevice> findByUserIdAndIsActive(@Param("userId") Long userId, @Param("isActive") boolean isActive);

    /**
     * Находит активное доверенное устройство по deviceId и userId
     */
    @Query("SELECT t FROM TrustedDevice t WHERE t.deviceId = :deviceId AND t.user.id = :userId AND t.isActive = :isActive")
    Optional<TrustedDevice> findByDeviceIdAndUserIdAndIsActive(@Param("deviceId") String deviceId,
                                                               @Param("userId") Long userId,
                                                               @Param("isActive") boolean isActive);

    /**
     * Находит устройство только по deviceId (для поиска пользователя)
     */
    @Query("SELECT t FROM TrustedDevice t WHERE t.deviceId = :deviceId AND t.isActive = true")
    Optional<TrustedDevice> findByDeviceId(@Param("deviceId") String deviceId);
}
