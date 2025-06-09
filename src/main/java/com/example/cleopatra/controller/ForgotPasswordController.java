package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.ConfirmationToken;
import com.example.cleopatra.service.ConfirmationTokenService;
import com.example.cleopatra.service.PasswordResetService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/forgot-password")
@Slf4j
public class ForgotPasswordController {

    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final ConfirmationTokenService confirmationTokenService;

    @GetMapping
    public String showForgotPasswordForm(Model model) {
        return "auth/forgot-password";
    }

    /**
     * ШАГ 1: Запрос на сброс пароля (отправляем письмо с подтверждением)
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestParam String email) {
        try {
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Пользователь не найден"));
            }

            // Отправляем письмо с ПОДТВЕРЖДЕНИЕМ (не новый пароль!)
            ConfirmationToken token = passwordResetService.requestPasswordReset(email, user.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Письмо с подтверждением сброса пароля отправлено",
                    "email", email,
                    "info", "Проверьте почту и подтвердите сброс пароля",
                    "expiresAt", token.getExpiresAt().toString()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during password reset request for email: {}", email, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ошибка при отправке письма"
            ));
        }
    }

    /**
     * ШАГ 2: Подтверждение сброса пароля (генерируем и отправляем новый пароль)
     */
    @GetMapping("/confirm-reset")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@RequestParam String token) {
        log.info("Received password reset confirmation request with token: {}", token);

        try {
            // Сначала проверим токен
            ConfirmationToken confirmationToken = confirmationTokenService.getToken(token);
            log.info("Token found: email={}, type={}, isValid={}, isExpired={}, isUsed={}",
                    confirmationToken.getEmail(),
                    confirmationToken.getTokenType(),
                    confirmationToken.isValid(),
                    confirmationToken.isExpired(),
                    confirmationToken.getIsUsed());

            // Подтверждаем сброс и получаем новый пароль
            String newPassword = passwordResetService.confirmPasswordReset(token);
            log.info("New password generated for email: {}", confirmationToken.getEmail());

            // Сохраняем новый пароль в БД
            userService.resetPasswordByEmail(confirmationToken.getEmail(), newPassword);
            log.info("Password successfully updated in database for email: {}", confirmationToken.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Пароль успешно сброшен! Новый пароль отправлен на почту",
                    "email", confirmationToken.getEmail(),
                    "info", "Проверьте почту для получения нового пароля"
            ));
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException during password reset confirmation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("IllegalStateException during password reset confirmation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during password reset confirmation with token: {}", token, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ошибка при подтверждении сброса пароля: " + e.getMessage()
            ));
        }
    }

    /**
     * Запрос на подтверждение email
     */
    @PostMapping("/confirm-request")
    public ResponseEntity<Map<String, String>> requestEmailConfirmation(@RequestParam String email) {
        try {
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Пользователь не найден"));
            }

            if (confirmationTokenService.hasActiveToken(email, ConfirmationToken.TokenType.EMAIL_CONFIRMATION)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "У вас уже есть активный токен подтверждения. Проверьте почту."
                ));
            }

            ConfirmationToken token = passwordResetService.sendEmailConfirmation(email, user.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Письмо для подтверждения отправлено",
                    "email", email,
                    "expiresAt", token.getExpiresAt().toString()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during email confirmation request for: {}", email, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ошибка при отправке письма"
            ));
        }
    }

    /**
     * Подтверждение email по токену
     */
    @GetMapping("/confirm-email")
    public ResponseEntity<Map<String, String>> confirmEmail(@RequestParam String token) {
        try {
            passwordResetService.confirmEmail(token);

            ConfirmationToken confirmationToken = confirmationTokenService.getToken(token);

            return ResponseEntity.ok(Map.of(
                    "message", "Email успешно подтвержден!",
                    "email", confirmationToken.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during email confirmation with token: {}", token, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ошибка при подтверждении email"
            ));
        }
    }

    /**
     * Проверка статуса токена (для отладки)
     */
    @GetMapping("/token-status")
    public ResponseEntity<Map<String, Object>> getTokenStatus(@RequestParam String token) {
        try {
            ConfirmationToken confirmationToken = confirmationTokenService.getToken(token);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "email", confirmationToken.getEmail(),
                    "type", confirmationToken.getTokenType(),
                    "isValid", confirmationToken.isValid(),
                    "isExpired", confirmationToken.isExpired(),
                    "isUsed", confirmationToken.getIsUsed(),
                    "createdAt", confirmationToken.getCreatedAt(),
                    "expiresAt", confirmationToken.getExpiresAt(),
                    "confirmedAt", confirmationToken.getConfirmedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Токен не найден"));
        }
    }
}