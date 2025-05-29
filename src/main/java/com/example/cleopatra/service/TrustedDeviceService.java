package com.example.cleopatra.service;
import com.example.cleopatra.model.TrustedDevice;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.TrustedDeviceRepository;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustedDeviceService {

    private final TrustedDeviceRepository trustedDeviceRepository;
    private final UserRepository userRepository;

    @Value("${app.trusted-devices.max-count:5}")
    private int maxTrustedDevicesPerUser;

    /**
     * Проверяет, является ли устройство доверенным для пользователя
     * @param deviceId ID устройства
     * @param userId ID пользователя
     * @return true если устройство доверенное
     */
    public boolean isDeviceTrusted(String deviceId, Long userId) {
        return trustedDeviceRepository.existsByDeviceIdAndUserIdAndIsActive(deviceId, userId);
    }

    /**
     * Регистрирует новое доверенное устройство
     * @param userId ID пользователя
     * @param deviceName название устройства
     * @param deviceFingerprint отпечаток устройства
     * @return ID нового устройства или null если превышен лимит
     */
    @Transactional
    public String registerTrustedDevice(Long userId, String deviceName, String deviceFingerprint) {
        // Проверяем лимит устройств
        int currentCount = trustedDeviceRepository.countByUserIdAndIsActive(userId, true);
        if (currentCount >= maxTrustedDevicesPerUser) {
            log.warn("User {} exceeded trusted devices limit: {}", userId, maxTrustedDevicesPerUser);
            return null;
        }

        // Проверяем существование пользователя
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("User not found: {}", userId);
            return null;
        }

        User user = userOpt.get();
        String deviceId = UUID.randomUUID().toString();

        // Создаем новое доверенное устройство
        TrustedDevice device = TrustedDevice.builder()
                .deviceId(deviceId)
                .deviceName(deviceName)
                .deviceFingerprint(deviceFingerprint)
                .user(user)
                .isActive(true)
                .build();

        trustedDeviceRepository.save(device);

        log.info("Registered trusted device for user {}: {} ({})", userId, deviceName, deviceId);

        return deviceId;
    }

    /**
     * Обновляет время последнего использования устройства
     * @param deviceId ID устройства
     */
    @Transactional
    public void updateLastUsed(String deviceId) {
        Optional<TrustedDevice> deviceOpt = trustedDeviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isPresent()) {
            TrustedDevice device = deviceOpt.get();
            device.setLastUsedAt(LocalDateTime.now());
            trustedDeviceRepository.save(device);
        }
    }

    /**
     * Получает список всех доверенных устройств пользователя
     * @param userId ID пользователя
     * @return список устройств
     */
    public List<TrustedDeviceDto> getTrustedDevices(Long userId) {
        List<TrustedDevice> devices = trustedDeviceRepository.findByUserIdAndIsActive(userId, true);

        return devices.stream()
                .map(device -> new TrustedDeviceDto(
                        device.getId(),
                        device.getDeviceId(),
                        device.getDeviceName(),
                        device.getLastUsedAt(),
                        device.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Отзывает доверие устройству
     * @param deviceId ID устройства
     * @param userId ID пользователя (для проверки прав)
     * @return true если устройство успешно отозвано
     */
    @Transactional
    public boolean revokeTrustedDevice(Long deviceId, Long userId) {
        Optional<TrustedDevice> deviceOpt = trustedDeviceRepository.findById(deviceId);

        if (deviceOpt.isEmpty()) {
            return false;
        }

        TrustedDevice device = deviceOpt.get();

        // Проверяем, что устройство принадлежит пользователю
        if (!device.getUser().getId().equals(userId)) {
            log.warn("User {} tried to revoke device {} owned by user {}",
                    userId, deviceId, device.getUser().getId());
            return false;
        }

        device.setIsActive(false);
        trustedDeviceRepository.save(device);

        log.info("User {} revoked trusted device: {}", userId, device.getDeviceName());
        return true;
    }

    /**
     * Отзывает доверие устройству по deviceId
     * @param deviceId UUID устройства
     * @param userId ID пользователя
     * @return true если успешно отозвано
     */
    @Transactional
    public boolean revokeTrustedDeviceByDeviceId(String deviceId, Long userId) {
        Optional<TrustedDevice> deviceOpt = trustedDeviceRepository
                .findByDeviceIdAndUserIdAndIsActive(deviceId, userId, true);

        if (deviceOpt.isEmpty()) {
            return false;
        }

        TrustedDevice device = deviceOpt.get();
        device.setIsActive(false);
        trustedDeviceRepository.save(device);

        log.info("User {} revoked trusted device by deviceId: {}", userId, deviceId);
        return true;
    }

    /**
     * Находит пользователя по deviceId доверенного устройства
     * @param deviceId UUID устройства
     * @return Optional<User> если устройство найдено и активно
     */
    public Optional<User> findUserByTrustedDevice(String deviceId) {
        try {
            Optional<TrustedDevice> deviceOpt = trustedDeviceRepository.findByDeviceId(deviceId);

            if (deviceOpt.isPresent()) {
                TrustedDevice device = deviceOpt.get();

                // Проверяем что устройство активно
                if (device.getIsActive()) {
                    log.info("Found trusted device for user: {} (device: {})",
                            device.getUser().getEmail(), device.getDeviceName());
                    return Optional.of(device.getUser());
                } else {
                    log.warn("Trusted device found but inactive: {}", deviceId);
                }
            } else {
                log.debug("Trusted device not found: {}", deviceId);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error finding user by trusted device: {}", deviceId, e);
            return Optional.empty();
        }
    }

    /**
     * Получает информацию о доверенном устройстве
     * @param deviceId ID устройства
     * @param userId ID пользователя
     * @return информация об устройстве
     */
    public Optional<TrustedDevice> getTrustedDevice(String deviceId, Long userId) {
        return trustedDeviceRepository.findByDeviceIdAndUserIdAndIsActive(deviceId, userId, true);
    }

    // DTO для передачи данных об устройствах
    public static class TrustedDeviceDto {
        private final Long id;
        private final String deviceId;
        private final String deviceName;
        private final LocalDateTime lastUsedAt;
        private final LocalDateTime createdAt;

        public TrustedDeviceDto(Long id, String deviceId, String deviceName,
                                LocalDateTime lastUsedAt, LocalDateTime createdAt) {
            this.id = id;
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.lastUsedAt = lastUsedAt;
            this.createdAt = createdAt;
        }

        // Getters
        public Long getId() { return id; }
        public String getDeviceId() { return deviceId; }
        public String getDeviceName() { return deviceName; }
        public LocalDateTime getLastUsedAt() { return lastUsedAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}