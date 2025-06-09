//package com.example.cleopatra.service;
//
//import com.example.cleopatra.model.ConfirmationToken;
//import com.example.cleopatra.service.ConfirmationTokenService;
//import com.example.cleopatra.service.EmailService;
//import com.example.cleopatra.service.PasswordGeneratorService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PasswordResetService {
//
//    private final EmailService emailService;
//    private final PasswordGeneratorService passwordGeneratorService;
//    private final ConfirmationTokenService confirmationTokenService;
//
//    @Value("${app.base-url:http://localhost:2027}")
//    private String baseUrl;
//
//    /**
//     * Отправляет письмо с новым сгенерированным паролем
//     */
//    public String sendPasswordResetEmail(String email) {
//        try {
//            String newPassword = passwordGeneratorService.generatePassword(10);
//            emailService.sendPasswordResetEmail(email, newPassword);
//            log.info("Password reset email sent successfully to: {}", email);
//            return newPassword;
//        } catch (Exception e) {
//            log.error("Failed to send password reset email to: {}", email, e);
//            throw new RuntimeException("Не удалось отправить письмо с новым паролем");
//        }
//    }
//
//    /**
//     * Отправляет письмо с новым паролем (если пароль уже сгенерирован)
//     */
//    public void sendPasswordResetEmail(String email, String newPassword) {
//        try {
//            emailService.sendPasswordResetEmail(email, newPassword);
//            log.info("Password reset email sent successfully to: {}", email);
//        } catch (Exception e) {
//            log.error("Failed to send password reset email to: {}", email, e);
//            throw new RuntimeException("Не удалось отправить письмо с новым паролем");
//        }
//    }
//
//    /**
//     * Отправляет письмо для подтверждения email с токеном
//     */
//    public ConfirmationToken sendEmailConfirmation(String email, Long userId) {
//        try {
//            // Проверяем, есть ли уже активный токен
//            if (confirmationTokenService.hasActiveToken(email, ConfirmationToken.TokenType.EMAIL_CONFIRMATION)) {
//                throw new IllegalStateException("У пользователя уже есть активный токен подтверждения");
//            }
//
//            // Создаем новый токен
//            ConfirmationToken token = confirmationTokenService.createEmailConfirmationToken(email, userId);
//
//            // Отправляем письмо
//            String confirmationLink = buildConfirmationLink(token.getToken());
//            emailService.sendEmailConfirmation(email, confirmationLink);
//
//            log.info("Email confirmation sent successfully to: {}", email);
//            return token;
//        } catch (Exception e) {
//            log.error("Failed to send email confirmation to: {}", email, e);
//            throw new RuntimeException("Не удалось отправить письмо для подтверждения");
//        }
//    }
//
//    /**
//     * Отправляет письмо для подтверждения email с готовым токеном (устаревший метод)
//     */
//    @Deprecated
//    public void sendEmailConfirmation(String email, String confirmationToken) {
//        try {
//            String confirmationLink = buildConfirmationLink(confirmationToken);
//            emailService.sendEmailConfirmation(email, confirmationLink);
//            log.info("Email confirmation sent successfully to: {}", email);
//        } catch (Exception e) {
//            log.error("Failed to send email confirmation to: {}", email, e);
//            throw new RuntimeException("Не удалось отправить письмо для подтверждения");
//        }
//    }
//
//    /**
//     * Подтверждает email по токену
//     */
//    public void confirmEmail(String token) {
//        if (!confirmationTokenService.validateToken(token)) {
//            throw new IllegalArgumentException("Недействительный или истекший токен");
//        }
//
//        confirmationTokenService.confirmToken(token);
//        log.info("Email confirmed successfully for token: {}", token);
//    }
//
//    /**
//     * Генерирует новый пароль
//     */
//    public String generateNewPassword() {
//        return passwordGeneratorService.generatePassword(10);
//    }
//
//    /**
//     * Генерирует надежный пароль
//     */
//    public String generateSecurePassword() {
//        return passwordGeneratorService.generateSecurePassword(12);
//    }
//
//    /**
//     * Строит ссылку для подтверждения email
//     */
//    private String buildConfirmationLink(String token) {
//        return baseUrl + "/forgot-password/confirm?token=" + token;
//    }
//}

package com.example.cleopatra.service;

import com.example.cleopatra.model.ConfirmationToken;
import com.example.cleopatra.service.ConfirmationTokenService;
import com.example.cleopatra.service.EmailService;
import com.example.cleopatra.service.PasswordGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final EmailService emailService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final ConfirmationTokenService confirmationTokenService;

    @Value("${app.base-url:http://localhost:2027}")
    private String baseUrl;

    /**
     * ШАГ 1: Запрос на сброс пароля (отправляем письмо с подтверждением)
     */
    public ConfirmationToken requestPasswordReset(String email, Long userId) {
        try {
            // Проверяем, есть ли уже активный токен сброса пароля
            if (confirmationTokenService.hasActiveToken(email, ConfirmationToken.TokenType.PASSWORD_RESET)) {
                throw new IllegalStateException("У пользователя уже есть активный запрос на сброс пароля");
            }

            // Создаем токен сброса пароля (живет 1 час)
            ConfirmationToken token = confirmationTokenService.createPasswordResetToken(email, userId);

            // Отправляем письмо с ПОДТВЕРЖДЕНИЕМ (не новый пароль!)
            String confirmationLink = buildPasswordResetConfirmationLink(token.getToken());
            sendPasswordResetConfirmationEmail(email, confirmationLink);

            log.info("Password reset confirmation email sent to: {}", email);
            return token;
        } catch (Exception e) {
            log.error("Failed to send password reset confirmation to: {}", email, e);
            throw new RuntimeException("Не удалось отправить письмо с подтверждением сброса пароля");
        }
    }

    /**
     * ШАГ 2: Подтверждение сброса пароля (генерируем новый пароль и отправляем)
     */
    public String confirmPasswordReset(String token) {
        try {
            // Валидируем токен
            if (!confirmationTokenService.validateToken(token)) {
                throw new IllegalArgumentException("Недействительный или истекший токен");
            }

            ConfirmationToken confirmationToken = confirmationTokenService.getToken(token);

            // Проверяем, что это именно токен сброса пароля
            if (confirmationToken.getTokenType() != ConfirmationToken.TokenType.PASSWORD_RESET) {
                throw new IllegalArgumentException("Неверный тип токена");
            }

            // Генерируем новый пароль
            String newPassword = passwordGeneratorService.generatePassword(12);

            // Отправляем письмо с новым паролем
            emailService.sendPasswordResetEmail(confirmationToken.getEmail(), newPassword);

            // Помечаем токен как использованный
            confirmationTokenService.confirmToken(token);

            log.info("Password reset confirmed and new password sent to: {}", confirmationToken.getEmail());
            return newPassword; // Возвращаем для сохранения в БД

        } catch (Exception e) {
            log.error("Failed to confirm password reset for token: {}", token, e);
            throw new RuntimeException("Не удалось подтвердить сброс пароля");
        }
    }

    /**
     * Отправляет письмо для подтверждения email
     */
    public ConfirmationToken sendEmailConfirmation(String email, Long userId) {
        try {
            if (confirmationTokenService.hasActiveToken(email, ConfirmationToken.TokenType.EMAIL_CONFIRMATION)) {
                throw new IllegalStateException("У пользователя уже есть активный токен подтверждения");
            }

            ConfirmationToken token = confirmationTokenService.createEmailConfirmationToken(email, userId);
            String confirmationLink = buildEmailConfirmationLink(token.getToken());
            emailService.sendEmailConfirmation(email, confirmationLink);

            log.info("Email confirmation sent successfully to: {}", email);
            return token;
        } catch (Exception e) {
            log.error("Failed to send email confirmation to: {}", email, e);
            throw new RuntimeException("Не удалось отправить письмо для подтверждения");
        }
    }

    /**
     * Подтверждает email по токену
     */
    public void confirmEmail(String token) {
        if (!confirmationTokenService.validateToken(token)) {
            throw new IllegalArgumentException("Недействительный или истекший токен");
        }

        ConfirmationToken confirmationToken = confirmationTokenService.getToken(token);
        if (confirmationToken.getTokenType() != ConfirmationToken.TokenType.EMAIL_CONFIRMATION) {
            throw new IllegalArgumentException("Неверный тип токена");
        }

        confirmationTokenService.confirmToken(token);
        log.info("Email confirmed successfully for: {}", confirmationToken.getEmail());
    }

    /**
     * Генерирует новый пароль
     */
    public String generateNewPassword() {
        return passwordGeneratorService.generatePassword(12);
    }

    /**
     * Генерирует надежный пароль
     */
    public String generateSecurePassword() {
        return passwordGeneratorService.generateSecurePassword(14);
    }

    /**
     * Отправляет письмо с подтверждением сброса пароля
     */
    private void sendPasswordResetConfirmationEmail(String email, String confirmationLink) {
        // Используем специальный шаблон для подтверждения сброса пароля
        String subject = "Подтверждение сброса пароля - Cleopatra";
        String htmlContent = buildPasswordResetConfirmationTemplate(confirmationLink);

        emailService.sendHtmlEmail(email, subject, htmlContent);
    }

    /**
     * Строит ссылку для подтверждения сброса пароля
     */
    // В PasswordResetService проверьте этот метод:

    private String buildPasswordResetConfirmationLink(String token) {
        String link = baseUrl + "/forgot-password/confirm-reset?token=" + token;
        log.info("Built password reset confirmation link: {}", link); // Добавьте эту строку для отладки
        return link;
    }

    /**
     * Строит ссылку для подтверждения email
     */
    private String buildEmailConfirmationLink(String token) {
        return baseUrl + "/forgot-password/confirm-email?token=" + token;
    }

    /**
     * HTML шаблон для подтверждения сброса пароля
     */
    private String buildPasswordResetConfirmationTemplate(String confirmationLink) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Подтверждение сброса пароля</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4; }
                    .container { background: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; border-bottom: 3px solid #D4AF37; padding-bottom: 20px; margin-bottom: 30px; }
                    .header h1 { color: #D4AF37; margin: 0; font-size: 28px; }
                    .warning { background: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .btn { display: inline-block; padding: 15px 30px; background: #D4AF37; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; margin: 20px 0; }
                    .footer { text-align: center; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Cleopatra</h1>
                        <p>Подтверждение сброса пароля</p>
                    </div>
                    
                    <div class="content">
                        <h2>Подтвердите сброс пароля</h2>
                        <p>Мы получили запрос на сброс пароля для вашего аккаунта.</p>
                        
                        <div class="warning">
                            <strong>⚠️ Важно:</strong>
                            <p>Если вы НЕ запрашивали сброс пароля, проигнорируйте это письмо. Ваш пароль останется без изменений.</p>
                        </div>
                        
                        <p>Чтобы подтвердить сброс пароля и получить новый пароль, нажмите кнопку ниже:</p>
                        
                        <p style="text-align: center;">
                            <a href="%s" class="btn">
                                ✅ Подтвердить сброс пароля
                            </a>
                        </p>
                        
                        <p><strong>⏰ Срок действия ссылки:</strong> 1 час</p>
                    </div>
                    
                    <div class="footer">
                        <p>Если кнопка не работает, скопируйте ссылку: %s</p>
                        <p>&copy; 2024 Cleopatra. Все права защищены.</p>
                    </div>
                </div>
            </body>
            </html>
            """, confirmationLink, confirmationLink);
    }
}