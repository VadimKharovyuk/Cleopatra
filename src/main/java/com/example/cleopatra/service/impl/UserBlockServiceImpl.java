package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.BlockedUse.BlockedUserDto;
import com.example.cleopatra.dto.BlockedUse.BlockedUsersPageResponse;
import com.example.cleopatra.dto.BlockedUse.UserBlockResponse;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.maper.UserBlockMapper;
import com.example.cleopatra.maper.UserMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.model.UserBlock;
import com.example.cleopatra.repository.UserBlockRepository;
import com.example.cleopatra.service.UserBlockService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserBlockServiceImpl implements UserBlockService {
    private final UserBlockRepository userBlockRepository;
    private final UserBlockMapper userBlockMapper;
    private final UserService userService;

    @Override
    @Transactional
    public UserBlockResponse blockUser(Long blockerId, Long blockedId) {
        log.info("Попытка заблокировать пользователя: {} блокирует {}", blockerId, blockedId);

        // Валидация
        if (blockerId.equals(blockedId)) {
            log.warn("Попытка заблокировать самого себя: {}", blockerId);
            throw new IllegalArgumentException("Нельзя заблокировать самого себя");
        }

        // Получаем пользователей
        User blocker = userService.findById(blockerId);
        User blocked = userService.findById(blockedId);

        // Проверяем, не заблокирован ли уже
        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            log.warn("Пользователь {} уже заблокирован пользователем {}", blockedId, blockerId);
            throw new IllegalStateException("Пользователь уже заблокирован");
        }

        // Создаем блокировку
        UserBlock userBlock = UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();

        UserBlock savedBlock = userBlockRepository.save(userBlock);
        log.info("Пользователь {} успешно заблокирован пользователем {}", blockedId, blockerId);

        return userBlockMapper.toUserBlockResponse(savedBlock);
    }

    @Override
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        log.info("Начало разблокировки: блокирующий={}, заблокированный={}", blockerId, blockedId);

        try {
            // 1. Валидация входных параметров
            if (blockerId == null) {
                log.error("blockerId is null");
                throw new IllegalArgumentException("ID блокирующего пользователя не может быть null");
            }

            if (blockedId == null) {
                log.error("blockedId is null");
                throw new IllegalArgumentException("ID заблокированного пользователя не может быть null");
            }

            log.info("Валидация параметров прошла успешно");

            // 2. Получение пользователей
            User blocker;
            User blocked;

            try {
                blocker = userService.findById(blockerId);
                log.info("Блокирующий пользователь найден: {}", blocker.getEmail());
            } catch (Exception e) {
                log.error("Ошибка при поиске блокирующего пользователя с ID {}", blockerId, e);
                throw new RuntimeException("Блокирующий пользователь не найден", e);
            }

            try {
                blocked = userService.findById(blockedId);
                log.info("Заблокированный пользователь найден: {}", blocked.getEmail());
            } catch (Exception e) {
                log.error("Ошибка при поиске заблокированного пользователя с ID {}", blockedId, e);
                throw new RuntimeException("Заблокированный пользователь не найден", e);
            }

            // 3. Поиск блокировки
            Optional<UserBlock> blockOpt;
            try {
                log.info("Поиск блокировки между пользователями {} и {}", blockerId, blockedId);
                blockOpt = userBlockRepository.findByBlockerAndBlocked(blocker, blocked);
                log.info("Результат поиска блокировки: {}", blockOpt.isPresent() ? "найдена" : "не найдена");
            } catch (Exception e) {
                log.error("Ошибка при поиске блокировки в репозитории", e);
                throw new RuntimeException("Ошибка при поиске блокировки", e);
            }

            // 4. Удаление блокировки
            if (blockOpt.isPresent()) {
                try {
                    UserBlock userBlock = blockOpt.get();
                    log.info("Найдена блокировка с ID: {}, дата создания: {}",
                            userBlock.getId(), userBlock.getBlockedAt());

                    userBlockRepository.delete(userBlock);
                    log.info("Блокировка успешно удалена из БД");

                    log.info("Пользователь {} успешно разблокирован пользователем {}", blockedId, blockerId);
                } catch (Exception e) {
                    log.error("Ошибка при удалении блокировки из БД", e);
                    throw new RuntimeException("Ошибка при удалении блокировки", e);
                }
            } else {
                log.warn("Блокировка не найдена: пользователь {} не блокировал пользователя {}", blockerId, blockedId);
                throw new IllegalStateException("Блокировка не найдена");
            }

        } catch (IllegalStateException | IllegalArgumentException e) {
            // Эти исключения пробрасываем как есть
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при разблокировке пользователя", e);
            throw new RuntimeException("Неожиданная ошибка при разблокировке", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserBlock> getBlockedUsersPage(User blocker, Pageable pageable) {
        if (blocker == null) {
            return Page.empty();
        }
        return userBlockRepository.findByBlockerOrderByBlockedAtDesc(blocker, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public BlockedUsersPageResponse getBlockedUsersPageResponse(User blocker, Pageable pageable) {
        if (blocker == null) {
            return BlockedUsersPageResponse.builder()
                    .blockedUsers(List.of())
                    .currentPage(0)
                    .totalPages(0)
                    .totalElements(0)
                    .pageSize(pageable.getPageSize())
                    .hasNext(false)
                    .hasPrevious(false)
                    .blockedUsersCount(0)
                    .build();
        }

        Page<UserBlock> page = getBlockedUsersPage(blocker, pageable);
        long totalBlockedCount = getBlockedUsersCount(blocker.getId());

        return userBlockMapper.toBlockedUsersPageResponse(page, totalBlockedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserDto> getBlockedUsersDto(User blocker) {
        if (blocker == null) {
            return List.of();
        }

        List<UserBlock> userBlocks = userBlockRepository.findByBlockerOrderByBlockedAtDesc(blocker);
        return userBlockMapper.toBlockedUserDtoList(userBlocks);
    }


    @Transactional(readOnly = true)
    @Override
    public boolean isBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            return false;
        }
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }


    @Transactional(readOnly = true)
    @Override
    public long getBlockedUsersCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        User user = userService.findById(userId);
        return userBlockRepository.countByBlocker(user);
    }
}