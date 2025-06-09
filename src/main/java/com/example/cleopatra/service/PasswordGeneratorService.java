package com.example.cleopatra.service;

public interface PasswordGeneratorService {
    /**
     * Генерирует случайный пароль
     * @param length длина пароля
     * @return сгенерированный пароль
     */
    String generatePassword(int length);

    /**
     * Генерирует пароль стандартной длины (8 символов)
     * @return сгенерированный пароль
     */
    String generatePassword();

    /**
     * Генерирует надежный пароль с буквами, цифрами и спецсимволами
     * @param length длина пароля
     * @return сгенерированный пароль
     */
    String generateSecurePassword(int length);
}
