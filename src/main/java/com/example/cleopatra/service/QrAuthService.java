package com.example.cleopatra.service;
import com.example.cleopatra.model.QrAuthSession;
import com.example.cleopatra.enums.QrAuthStatus;
import com.example.cleopatra.repository.QrAuthSessionRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrAuthService {

    private final QrAuthSessionRepository qrAuthSessionRepository;

    @Value("${app.base-url:http://localhost:2027}")
    private String baseUrl;

    /**
     * Генерирует новую QR-сессию для авторизации
     * @return объект с токеном и ссылкой для QR-кода
     */
    @Transactional
    public QrSessionData generateQrSession() {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        QrAuthSession session = QrAuthSession.builder()
                .token(token)
                .status(QrAuthStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plusMinutes(5)) // минут вместо 5
                .build();

        qrAuthSessionRepository.save(session);

        String qrUrl = baseUrl + "/auth/qr/" + token;
        log.info("Generated QR session with token: {}", token);

        return new QrSessionData(token, qrUrl);
    }

    /**
     * Подтверждает QR-авторизацию от имени пользователя
     * @param token токен QR-сессии
     * @param userId ID пользователя, который подтверждает вход
     * @return true если подтверждение успешно
     */
    @Transactional
    public boolean confirmQrAuth(String token, Long userId) {
        log.info("Attempting to confirm QR session: {} for user: {}", token, userId);

        // Проверяем, что сессия существует и активна
        Optional<QrAuthSession> sessionOpt = qrAuthSessionRepository
                .findActiveByToken(token, LocalDateTime.now());

        if (sessionOpt.isEmpty()) {
            log.warn("Attempt to confirm expired or non-existent QR session: {}", token);
            return false;
        }

        QrAuthSession session = sessionOpt.get();
        log.info("Found QR session with status: {}", session.getStatus());

        // Проверяем статус
        if (session.getStatus() != QrAuthStatus.PENDING) {
            log.warn("Attempt to confirm QR session with status: {}", session.getStatus());
            return false;
        }

        int updated = qrAuthSessionRepository.confirmSession(token, userId);

        if (updated > 0) {
            log.info("QR session confirmed successfully for user: {} with token: {}", userId, token);
            return true;
        } else {
            log.error("Failed to update QR session in database");
            return false;
        }
    }

    /**
     * Проверяет статус QR-сессии (для polling)
     * @param token токен сессии
     * @return данные о статусе сессии
     */
    public QrStatusData checkQrStatus(String token) {
        Optional<QrAuthSession> sessionOpt = qrAuthSessionRepository.findByToken(token);

        if (sessionOpt.isEmpty()) {
            return new QrStatusData(QrAuthStatus.EXPIRED, null);
        }

        QrAuthSession session = sessionOpt.get();

        // Проверяем истечение времени
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Помечаем как истекшую
            session.setStatus(QrAuthStatus.EXPIRED);
            qrAuthSessionRepository.save(session);
            return new QrStatusData(QrAuthStatus.EXPIRED, null);
        }

        return new QrStatusData(session.getStatus(), session.getUserId());
    }

    /**
     * Получает информацию о QR-сессии по токену
     * @param token токен сессии
     * @return сессия если существует и активна
     */
    public Optional<QrAuthSession> getActiveSession(String token) {
        return qrAuthSessionRepository.findActiveByToken(token, LocalDateTime.now());
    }

    /**
     * Scheduled задача для очистки просроченных сессий
     * Запускается каждые 10 минут
     */
    @Scheduled(fixedRate = 600000) // 10 минут
    @Transactional
    public void cleanupExpiredSessions() {
        long startTime = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Сначала помечаем просроченные как EXPIRED
            int markedExpired = qrAuthSessionRepository.markExpiredSessions(now);

            // Затем удаляем старые записи (старше 1 часа)
            int deleted = qrAuthSessionRepository.deleteExpiredSessions(now.minusHours(1));

            long executionTime = System.currentTimeMillis() - startTime;

            if (markedExpired > 0 || deleted > 0) {
                log.info("QR session cleanup completed: marked {} as expired, deleted {} old sessions in {}ms",
                        markedExpired, deleted, executionTime);
            } else {
                log.debug("QR session cleanup completed: no sessions to clean in {}ms", executionTime);
            }

            // Дополнительная статистика (опционально)
            if (log.isDebugEnabled()) {
                int totalActive = qrAuthSessionRepository.countByStatus(QrAuthStatus.PENDING);
                int totalConfirmed = qrAuthSessionRepository.countByStatus(QrAuthStatus.CONFIRMED);
                log.debug("Current QR sessions: {} pending, {} confirmed", totalActive, totalConfirmed);
            }

        } catch (Exception e) {
            log.error("Error during QR session cleanup: {}", e.getMessage(), e);
            // Не пробрасываем исключение, чтобы не сломать планировщик
        }
    }

    // DTO классы для возврата данных

    @Getter
    public static class QrSessionData {
        private final String token;
        private final String qrUrl;

        public QrSessionData(String token, String qrUrl) {
            this.token = token;
            this.qrUrl = qrUrl;
        }

    }

    @Getter
    public static class QrStatusData {
        private final QrAuthStatus status;
        private final Long userId;

        public QrStatusData(QrAuthStatus status, Long userId) {
            this.status = status;
            this.userId = userId;
        }

        public boolean isConfirmed() { return status == QrAuthStatus.CONFIRMED; }
    }

}