package com.example.cleopatra.service;

import com.example.cleopatra.enums.ProfileAccessLevel;

public interface ProfileAccessService {

    /**
     * Проверяет, может ли пользователь просматривать профиль
     *
     * @param viewerId ID пользователя, который хочет просмотреть профиль (может быть null для неавторизованных)
     * @param profileOwnerId ID владельца профиля
     * @return true, если доступ разрешен
     */
    boolean canViewProfile(Long viewerId, Long profileOwnerId);

    /**
     * Проверяет, может ли пользователь просматривать конкретную часть профиля
     */
    boolean canViewProfileSection(Long viewerId, Long profileOwnerId, String section);

    /**
     * Возвращает сообщение для пользователя при отказе в доступе
     */
    String getAccessDeniedMessage(Long viewerId, Long profileOwnerId);

    /**
     * Проверяет, может ли пользователь изменить уровень доступа к профилю
     */
    boolean canChangeAccessLevel(Long userId, Long profileOwnerId);

    /**
     * Изменяет уровень доступа к профилю
     *
     * @param userId ID пользователя, который изменяет настройки
     * @param profileOwnerId ID владельца профиля
     * @param newAccessLevel новый уровень доступа
     * @return true, если изменение успешно
     */
    boolean updateProfileAccessLevel(Long userId, Long profileOwnerId, ProfileAccessLevel newAccessLevel);

    /**
     * Получает текущий уровень доступа к профилю
     */
    ProfileAccessLevel getProfileAccessLevel(Long profileOwnerId);

    /**
     * Проверяет, подписан ли subscriber на subscribedTo
     */
    boolean isSubscribedTo(Long subscriberId, Long subscribedToId);

    /**
     * Логирует попытку доступа к закрытому профилю
     */
    void logAccessAttempt(Long viewerId, Long profileOwnerId, boolean accessGranted);

    /**
     * Обновляет уровень доступа к фото
     */
    boolean updatePhotosAccessLevel(Long userId, Long profileOwnerId, ProfileAccessLevel newAccessLevel);

    /**
     * Обновляет уровень доступа к постам
     */
    boolean updatePostsAccessLevel(Long userId, Long profileOwnerId, ProfileAccessLevel newAccessLevel);

    /**
     * Получает уровень доступа к фото
     */
    ProfileAccessLevel getPhotosAccessLevel(Long profileOwnerId);

    /**
     * Получает уровень доступа к постам
     */
    ProfileAccessLevel getPostsAccessLevel(Long profileOwnerId);
}