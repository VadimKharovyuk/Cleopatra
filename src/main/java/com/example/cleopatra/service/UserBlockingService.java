package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.SystemBlock;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.SystemBlockRepository;
import com.example.cleopatra.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserBlockingService {

    private final UserRepository userRepository;
    private final SystemBlockRepository systemBlockRepository;

    /**
     * Проверяет, заблокирован ли пользователь по email
     */
    public boolean isUserBlocked(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        return user != null && Boolean.TRUE.equals(user.getIsBlocked());
    }

    /**
     * Проверяет, заблокирован ли пользователь по ID
     */
    public boolean isUserBlocked(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && Boolean.TRUE.equals(user.getIsBlocked());
    }

    /**
     * Блокирует пользователя
     */
    @Transactional
    public void blockUser(Long userId, String reason, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found with ID: " + adminId));

        // Проверяем, не заблокирован ли уже пользователь
        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new IllegalStateException("User is already blocked");
        }

        // Проверяем, что админ не блокирует сам себя
        if (userId.equals(adminId)) {
            throw new IllegalStateException("Admin cannot block themselves");
        }

        LocalDateTime now = LocalDateTime.now();

        // Обновляем пользователя
        user.setIsBlocked(true);
        user.setBlockedAt(now);
        user.setBlockReason(reason);
        user.setBlockedByAdminId(adminId);

        userRepository.save(user);

        // Создаем запись в истории блокировок
        SystemBlock systemBlock = SystemBlock.builder()
                .blockedUser(user)
                .blockedByAdmin(admin)
                .blockReason(reason)
                .blockedAt(now)
                .isActive(true)
                .build();

        systemBlockRepository.save(systemBlock);

        log.info("User {} (ID: {}) blocked by admin {} (ID: {}). Reason: {}",
                user.getEmail(), userId, admin.getEmail(), adminId, reason);
    }

    /**
     * Разблокирует пользователя
     */
    @Transactional
    public void unblockUser(Long userId, String reason, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found with ID: " + adminId));

        // Проверяем, заблокирован ли пользователь
        if (!Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new IllegalStateException("User is not blocked");
        }

        // Обновляем пользователя
        user.setIsBlocked(false);
        user.setBlockedAt(null);
        user.setBlockReason(null);
        user.setBlockedByAdminId(null);

        userRepository.save(user);

        // Находим активную блокировку и деактивируем её
        SystemBlock activeBlock = systemBlockRepository
                .findByBlockedUserAndIsActiveTrue(user)
                .orElse(null);

        if (activeBlock != null) {
            activeBlock.setIsActive(false);
            activeBlock.setUnblockedAt(LocalDateTime.now());
            activeBlock.setUnblockedByAdmin(admin);
            activeBlock.setUnblockReason(reason);

            systemBlockRepository.save(activeBlock);
        }

        log.info("User {} (ID: {}) unblocked by admin {} (ID: {}). Reason: {}",
                user.getEmail(), userId, admin.getEmail(), adminId, reason);
    }

    /**
     * Получает историю блокировок пользователя
     */
    public List<SystemBlock> getUserBlockHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        return systemBlockRepository.findByBlockedUserOrderByBlockedAtDesc(user);
    }

    /**
     * Получает список активных блокировок с пагинацией
     */
    public Page<SystemBlock> getActiveBlocks(Pageable pageable) {
        return systemBlockRepository.findByIsActiveTrueOrderByBlockedAtDesc(pageable);
    }

    /**
     * Получает все активные блокировки
     */
    public List<SystemBlock> getAllActiveBlocks() {
        return systemBlockRepository.findByIsActiveTrueOrderByBlockedAtDesc();
    }

    /**
     * Получает статистику блокировок за период
     */
    public Long getBlocksCountForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return systemBlockRepository.countBlocksBetweenDates(startDate, endDate);
    }

    /**
     * Получает информацию о текущей блокировке пользователя
     */
    public SystemBlock getCurrentBlock(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        return systemBlockRepository.findByBlockedUserAndIsActiveTrue(user).orElse(null);
    }

    public UserResponse findUserByEmailOrName(String query) {
        User user = userRepository.findByEmail(query).orElse(null);

        // Если не найден по email, ищем по имени
        if (user == null) {
            user = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        if (user == null) {
            throw new EntityNotFoundException("User not found with query: " + query);
        }

        return convertToUserResponse(user);
    }
    // Метод для конвертации User в UserResponse
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .isPrivateProfile(user.getIsPrivateProfile())
                .isBlocked(user.getIsBlocked())
                .build();
    }

    /**
     * Проверяет, может ли администратор выполнить операцию блокировки
     */
    public boolean canAdminBlockUser(Long adminId, Long userId) {
        // Админ не может блокировать сам себя
        if (adminId.equals(userId)) {
            return false;
        }

        // Можно добавить дополнительные проверки:
        // - проверка роли администратора
        // - проверка уровня доступа
        // - проверка что пользователь не является супер-админом и т.д.

        return true;
    }

    public UserResponse findUserByIdAsResponse(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        return convertToUserResponse(user);
    }
}