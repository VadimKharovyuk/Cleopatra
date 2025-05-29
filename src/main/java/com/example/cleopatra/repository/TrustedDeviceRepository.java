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

    // Найти устройство по deviceId и пользователю
    Optional<TrustedDevice> findByDeviceIdAndUserIdAndIsActive(
            String deviceId, Long userId, Boolean isActive
    );

    // Все активные устройства пользователя
    List<TrustedDevice> findByUserIdAndIsActive(Long userId, Boolean isActive);

    // Подсчет активных устройств пользователя (для лимита)
    int countByUserIdAndIsActive(Long userId, Boolean isActive);

    // Найти устройство для обновления lastUsedAt
    Optional<TrustedDevice> findByDeviceId(String deviceId);

    // Проверить существование активного устройства
    @Query("SELECT CASE WHEN COUNT(td) > 0 THEN true ELSE false END " +
            "FROM TrustedDevice td WHERE td.deviceId = :deviceId " +
            "AND td.user.id = :userId AND td.isActive = true")
    boolean existsByDeviceIdAndUserIdAndIsActive(
            @Param("deviceId") String deviceId,
            @Param("userId") Long userId
    );
}
