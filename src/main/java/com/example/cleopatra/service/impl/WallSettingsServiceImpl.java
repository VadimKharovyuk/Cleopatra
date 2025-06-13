package com.example.cleopatra.service.impl;//package com.example.cleopatra.service.impl;
//
//import com.example.cleopatra.dto.WallPost.WallSettingsResponse;
//import com.example.cleopatra.dto.WallPost.WallSettingsUpdateRequest;
//import com.example.cleopatra.enums.WallAccessLevel;
//import com.example.cleopatra.model.User;
//import com.example.cleopatra.repository.UserRepository;
//import com.example.cleopatra.service.WallSettingsService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@Transactional
//@RequiredArgsConstructor
//@Slf4j
//public class WallSettingsServiceImpl implements WallSettingsService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    @Transactional(readOnly = true)
//    public WallSettingsResponse getWallSettings(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
//
//        return WallSettingsResponse.builder()
//                .userId(user.getId())
//                .wallAccessLevel(user.getWallAccessLevel())
//                .displayName(user.getWallAccessLevel().getDisplayName())
//                .build();
//    }
//
//    @Override
//    public WallSettingsResponse updateWallAccessLevel(Long userId, WallAccessLevel accessLevel, Long currentUserId) {
//        // Проверяем права доступа
//        if (!canEditWallSettings(userId, currentUserId)) {
//            throw new AccessDeniedException("Нет прав для изменения настроек стены");
//        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
//
//        // Сохраняем старое значение для логирования
//        WallAccessLevel oldLevel = user.getWallAccessLevel();
//
//        // Обновляем уровень доступа
//        user.setWallAccessLevel(accessLevel);
//        User savedUser = userRepository.save(user);
//
//        log.info("Настройки стены обновлены для пользователя {}: {} -> {}",
//                userId, oldLevel, accessLevel);
//
//        return WallSettingsResponse.builder()
//                .userId(savedUser.getId())
//                .wallAccessLevel(savedUser.getWallAccessLevel())
//                .displayName(savedUser.getWallAccessLevel().getDisplayName())
//                .build();
//    }
//
//    @Override
//    public WallSettingsResponse updateWallSettings(Long userId, WallSettingsUpdateRequest request, Long currentUserId) {
//        // Проверяем права доступа
//        if (!canEditWallSettings(userId, currentUserId)) {
//            throw new AccessDeniedException("Нет прав для изменения настроек стены");
//        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
//
//        // Обновляем настройки
//        if (request.getWallAccessLevel() != null) {
//            user.setWallAccessLevel(request.getWallAccessLevel());
//        }
//
//        // Здесь можно добавить другие настройки стены в будущем
//        // if (request.getAllowComments() != null) {
//        //     user.setAllowCommentsOnWall(request.getAllowComments());
//        // }
//
//        User savedUser = userRepository.save(user);
//
//        log.info("Настройки стены обновлены для пользователя {}", userId);
//
//        return WallSettingsResponse.builder()
//                .userId(savedUser.getId())
//                .wallAccessLevel(savedUser.getWallAccessLevel())
//                .displayName(savedUser.getWallAccessLevel().getDisplayName())
//                .build();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canEditWallSettings(Long wallOwnerId, Long currentUserId) {
//        // Только владелец стены может изменять настройки
//        return wallOwnerId.equals(currentUserId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public WallAccessLevel[] getAllAccessLevels() {
//        return WallAccessLevel.values();
//    }
//}

import com.example.cleopatra.dto.WallPost.WallSettingsResponse;
import com.example.cleopatra.dto.WallPost.WallSettingsUpdateRequest;
import com.example.cleopatra.enums.WallAccessLevel;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.WallSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WallSettingsServiceImpl implements WallSettingsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public WallSettingsResponse getWallSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        return WallSettingsResponse.builder()
                .userId(user.getId())
                .wallAccessLevel(user.getWallAccessLevel())
                .displayName(user.getWallAccessLevel().getDisplayName())
                .build();
    }

    @Override
    public WallSettingsResponse updateWallAccessLevel(Long userId, WallAccessLevel accessLevel, Long currentUserId) {
        // Проверяем права доступа
        if (!canEditWallSettings(userId, currentUserId)) {
            throw new AccessDeniedException("Нет прав для изменения настроек стены");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        // Сохраняем старое значение для логирования
        WallAccessLevel oldLevel = user.getWallAccessLevel();

        // Обновляем уровень доступа
        user.setWallAccessLevel(accessLevel);
        User savedUser = userRepository.save(user);

        log.info("Настройки стены обновлены для пользователя {}: {} -> {}",
                userId, oldLevel, accessLevel);

        return WallSettingsResponse.builder()
                .userId(savedUser.getId())
                .wallAccessLevel(savedUser.getWallAccessLevel())
                .displayName(savedUser.getWallAccessLevel().getDisplayName())
                .build();
    }

    @Override
    public WallSettingsResponse updateWallSettings(Long userId, WallSettingsUpdateRequest request, Long currentUserId) {
        // Проверяем права доступа
        if (!canEditWallSettings(userId, currentUserId)) {
            throw new AccessDeniedException("Нет прав для изменения настроек стены");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        // Обновляем настройки
        if (request.getWallAccessLevel() != null) {
            user.setWallAccessLevel(request.getWallAccessLevel());
        }

        // Здесь можно добавить другие настройки стены в будущем
        // if (request.getAllowComments() != null) {
        //     user.setAllowCommentsOnWall(request.getAllowComments());
        // }

        User savedUser = userRepository.save(user);

        log.info("Настройки стены обновлены для пользователя {}", userId);

        return WallSettingsResponse.builder()
                .userId(savedUser.getId())
                .wallAccessLevel(savedUser.getWallAccessLevel())
                .displayName(savedUser.getWallAccessLevel().getDisplayName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditWallSettings(Long wallOwnerId, Long currentUserId) {
        // Только владелец стены может изменять настройки
        return wallOwnerId.equals(currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public WallAccessLevel[] getAllAccessLevels() {
        return WallAccessLevel.values();
    }
}