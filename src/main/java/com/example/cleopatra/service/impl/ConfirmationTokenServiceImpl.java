package com.example.cleopatra.service.impl;

import com.example.cleopatra.model.ConfirmationToken;
import com.example.cleopatra.repository.ConfirmationTokenRepository;
import com.example.cleopatra.service.ConfirmationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmationTokenServiceImpl implements ConfirmationTokenService {

    private static final int EMAIL_TOKEN_EXPIRY_HOURS = 24;
    private static final int PASSWORD_RESET_TOKEN_EXPIRY_HOURS = 1;

    private final ConfirmationTokenRepository tokenRepository;

    @Override
    @Transactional
    public ConfirmationToken createEmailConfirmationToken(String email, Long userId) {
        // Деактивируем старые токены
        deactivateOldTokens(email, ConfirmationToken.TokenType.EMAIL_CONFIRMATION);

        // Создаем новый токен
        String token = generateToken();
        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                email,
                userId,
                ConfirmationToken.TokenType.EMAIL_CONFIRMATION,
                EMAIL_TOKEN_EXPIRY_HOURS
        );

        ConfirmationToken saved = tokenRepository.save(confirmationToken);
        log.info("Created email confirmation token for user: {}", email);
        return saved;
    }

    @Override
    @Transactional
    public ConfirmationToken createPasswordResetToken(String email, Long userId) {
        // Деактивируем старые токены
        deactivateOldTokens(email, ConfirmationToken.TokenType.PASSWORD_RESET);

        // Создаем новый токен
        String token = generateToken();
        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                email,
                userId,
                ConfirmationToken.TokenType.PASSWORD_RESET,
                PASSWORD_RESET_TOKEN_EXPIRY_HOURS
        );

        ConfirmationToken saved = tokenRepository.save(confirmationToken);
        log.info("Created password reset token for user: {}", email);
        return saved;
    }

    @Override
    public boolean validateToken(String token) {
        Optional<ConfirmationToken> confirmationToken = tokenRepository.findByToken(token);

        if (confirmationToken.isEmpty()) {
            log.warn("Token not found: {}", token);
            return false;
        }

        ConfirmationToken tokenEntity = confirmationToken.get();

        if (!tokenEntity.isValid()) {
            log.warn("Invalid token: {} - used: {}, expired: {}",
                    token, tokenEntity.getIsUsed(), tokenEntity.isExpired());
            return false;
        }

        return true;
    }

    @Override
    public ConfirmationToken getToken(String token) {
        log.info("Searching for token: {}", token);
        Optional<ConfirmationToken> result = tokenRepository.findByToken(token);
        log.info("Token found: {}", result.isPresent());
        if (result.isPresent()) {
            ConfirmationToken foundToken = result.get();
            log.info("Token details: email={}, type={}, isValid={}",
                    foundToken.getEmail(), foundToken.getTokenType(), foundToken.isValid());
        }
        return result.orElseThrow(() -> new IllegalArgumentException("Токен не найден"));
    }

    @Override
    @Transactional
    public void confirmToken(String token) {
        ConfirmationToken confirmationToken = getToken(token);

        if (!confirmationToken.isValid()) {
            throw new IllegalStateException("Токен недействителен или уже использован");
        }

        confirmationToken.markAsUsed();
        tokenRepository.save(confirmationToken);

        log.info("Token confirmed for email: {}", confirmationToken.getEmail());
    }

    @Override
    public boolean hasActiveToken(String email, ConfirmationToken.TokenType tokenType) {
        return tokenRepository.existsActiveToken(email, tokenType, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void deactivateOldTokens(String email, ConfirmationToken.TokenType tokenType) {
        tokenRepository.deactivateAllTokensByEmailAndType(email, tokenType);
        log.debug("Deactivated old tokens for email: {} and type: {}", email, tokenType);
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 3600000) // Каждый час
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        var expiredTokens = tokenRepository.findExpiredTokens(now);

        if (!expiredTokens.isEmpty()) {
            tokenRepository.deleteExpiredTokens(now);
            log.info("Cleaned up {} expired tokens", expiredTokens.size());
        }
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}