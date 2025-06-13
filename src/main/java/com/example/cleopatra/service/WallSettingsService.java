package com.example.cleopatra.service;

import com.example.cleopatra.dto.WallPost.WallSettingsResponse;
import com.example.cleopatra.dto.WallPost.WallSettingsUpdateRequest;
import com.example.cleopatra.enums.WallAccessLevel;

public interface WallSettingsService {

    /**
     * Получить настройки стены пользователя
     */
    WallSettingsResponse getWallSettings(Long userId);

    /**
     * Обновить уровень доступа к стене
     */
    WallSettingsResponse updateWallAccessLevel(Long userId, WallAccessLevel accessLevel, Long currentUserId);

    /**
     * Обновить все настройки стены
     */
    WallSettingsResponse updateWallSettings(Long userId, WallSettingsUpdateRequest request, Long currentUserId);

    /**
     * Проверить, может ли пользователь изменять настройки стены
     */
    boolean canEditWallSettings(Long wallOwnerId, Long currentUserId);

    /**
     * Получить все доступные уровни доступа
     */
    WallAccessLevel[] getAllAccessLevels();
}