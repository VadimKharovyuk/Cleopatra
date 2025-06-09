package com.example.cleopatra.service.impl;

import com.example.cleopatra.service.PasswordGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@Slf4j
public class PasswordGeneratorServiceImpl implements PasswordGeneratorService {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

    private static final String DIGITS = "0123456789";


    private static final String SIMPLE_CHARS = LOWERCASE +  DIGITS;
    private static final String SECURE_CHARS = LOWERCASE +  DIGITS ;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generatePassword(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Password length must be positive");
        }

        return generateRandomString(SIMPLE_CHARS, length);
    }

    @Override
    public String generatePassword() {
        return generatePassword(8); // Стандартная длина 8 символов
    }

    @Override
    public String generateSecurePassword(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Secure password must be at least 4 characters long");
        }

        StringBuilder password = new StringBuilder();

        // Гарантируем хотя бы один символ каждого типа
        password.append(getRandomChar(LOWERCASE));
        password.append(getRandomChar(DIGITS));
        // Заполняем остальные позиции случайными символами
        for (int i = 4; i < length; i++) {
            password.append(getRandomChar(SECURE_CHARS));
        }

        // Перемешиваем символы для большей случайности
        return shuffleString(password.toString());
    }

    private String generateRandomString(String chars, int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(getRandomChar(chars));
        }
        return result.toString();
    }

    private char getRandomChar(String chars) {
        return chars.charAt(random.nextInt(chars.length()));
    }

    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int randomIndex = random.nextInt(chars.length);
            char temp = chars[i];
            chars[i] = chars[randomIndex];
            chars[randomIndex] = temp;
        }
        return new String(chars);
    }
}
