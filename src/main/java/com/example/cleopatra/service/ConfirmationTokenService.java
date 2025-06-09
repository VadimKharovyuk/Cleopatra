package com.example.cleopatra.service;

import com.example.cleopatra.model.ConfirmationToken;

public interface ConfirmationTokenService {

    /**
     * Создать и сохранить токен подтверждения email
     */
    ConfirmationToken createEmailConfirmationToken(String email, Long userId);

    /**
     * Создать и сохранить токен сброса пароля
     */
    ConfirmationToken createPasswordResetToken(String email, Long userId);

    /**
     * Валидировать токен
     */
    boolean validateToken(String token);

    /**
     * Получить токен по значению
     */
    ConfirmationToken getToken(String token);

    /**
     * Подтвердить токен (пометить как использованный)
     */
    void confirmToken(String token);

    /**
     * Проверить, есть ли активный токен у пользователя
     */
    boolean hasActiveToken(String email, ConfirmationToken.TokenType tokenType);

    /**
     * Деактивировать все старые токены пользователя
     */
    void deactivateOldTokens(String email, ConfirmationToken.TokenType tokenType);

    /**
     * Очистить истекшие токены
     */
    void cleanupExpiredTokens();
}