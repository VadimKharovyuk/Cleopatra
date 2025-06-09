package com.example.cleopatra.service.impl;

import com.example.cleopatra.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailQueueManager implements EmailService {

    private final JavaMailSender mailSender;
    private final Queue<EmailTask> emailQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger processingCount = new AtomicInteger(0);

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Cleopatra}")
    private String appName;

    @Override
    public void sendPasswordResetEmail(String toEmail, String newPassword) {
        queuePasswordResetEmail(toEmail, newPassword);
    }

    @Override
    public void sendEmailConfirmation(String toEmail, String confirmationLink) {
        queueEmailConfirmation(toEmail, confirmationLink);
    }

    @Override
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        queueHtmlEmail(toEmail, subject, htmlContent);
    }


    public void queuePasswordResetEmail(String toEmail, String newPassword) {
        String subject = "Восстановление пароля - " + appName;
        String htmlContent = buildPasswordResetEmailTemplate(newPassword);

        EmailTask task = new EmailTask(toEmail, subject, htmlContent, "password_reset", LocalDateTime.now());
        emailQueue.offer(task);

        log.info("Password reset email queued for: {}. Queue size: {}", toEmail, emailQueue.size());
    }

    public void queueEmailConfirmation(String toEmail, String confirmationLink) {
        String subject = "Подтверждение email - " + appName;
        String htmlContent = buildEmailConfirmationTemplate(confirmationLink);

        EmailTask task = new EmailTask(toEmail, subject, htmlContent, "email_confirmation", LocalDateTime.now());
        emailQueue.offer(task);

        log.info("Email confirmation queued for: {}. Queue size: {}", toEmail, emailQueue.size());
    }

    /**
     * Добавляет произвольное HTML письмо в очередь
     */
    public void queueHtmlEmail(String toEmail, String subject, String htmlContent) {
        EmailTask task = new EmailTask(toEmail, subject, htmlContent, "html_email", LocalDateTime.now());
        emailQueue.offer(task);

        log.info("HTML email queued for: {}. Subject: '{}'. Queue size: {}", toEmail, subject, emailQueue.size());
    }

    // Обработчик очереди - каждые 10 секунд
    @Scheduled(fixedDelay = 10000)
    public void processEmailQueue() {
        // Ограничиваем количество одновременно обрабатываемых писем
        if (processingCount.get() >= 3) {
            return;
        }

        EmailTask task = emailQueue.poll();
        if (task != null) {
            processingCount.incrementAndGet();

            CompletableFuture.runAsync(() -> {
                try {
                    sendEmailSync(task);
                    log.info("Email sent to: {}. Type: {}. Remaining queue: {}",
                            task.getToEmail(), task.getType(), emailQueue.size());
                } catch (Exception e) {
                    log.error("Failed to send email to: {}. Type: {}", task.getToEmail(), task.getType(), e);
                    // Можно добавить retry логику
                } finally {
                    processingCount.decrementAndGet();
                }
            });
        }
    }

    private void sendEmailSync(EmailTask task) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(task.getToEmail());
            helper.setSubject(task.getSubject());
            helper.setText(task.getHtmlContent(), true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Не удалось отправить письмо", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // Геттеры для мониторинга
    public int getQueueSize() {
        return emailQueue.size();
    }

    public int getProcessingCount() {
        return processingCount.get();
    }

    /**
     * HTML шаблон для восстановления пароля
     */
    private String buildPasswordResetEmailTemplate(String newPassword) {
        return String.format("""
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Восстановление пароля</title>
            <style>
                body {
                    font-family: 'Arial', sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f4f4f4;
                }
                .container {
                    background: #ffffff;
                    padding: 30px;
                    border-radius: 10px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }
                .header {
                    text-align: center;
                    border-bottom: 3px solid #D4AF37;
                    padding-bottom: 20px;
                    margin-bottom: 30px;
                }
                .header h1 {
                    color: #D4AF37;
                    margin: 0;
                    font-size: 28px;
                }
                .content {
                    margin-bottom: 30px;
                }
                .password-box {
                    background: #f8f9fa;
                    border: 2px dashed #D4AF37;
                    border-radius: 8px;
                    padding: 20px;
                    text-align: center;
                    margin: 20px 0;
                }
                .password-box h3 {
                    color: #D4AF37;
                    margin-top: 0;
                }
                .password {
                    font-size: 24px;
                    font-weight: bold;
                    background: #D4AF37;
                    color: white;
                    padding: 10px 20px;
                    border-radius: 5px;
                    display: inline-block;
                    letter-spacing: 2px;
                    margin: 10px 0;
                }
                .warning {
                    background: #fff3cd;
                    border: 1px solid #ffeaa7;
                    color: #856404;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 20px 0;
                }
                .footer {
                    text-align: center;
                    padding-top: 20px;
                    border-top: 1px solid #eee;
                    color: #666;
                    font-size: 14px;
                }
                .btn {
                    display: inline-block;
                    padding: 12px 24px;
                    background: #D4AF37;
                    color: white;
                    text-decoration: none;
                    border-radius: 5px;
                    font-weight: bold;
                    margin: 10px 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🔐 %s</h1>
                    <p>Восстановление пароля</p>
                </div>
                
                <div class="content">
                    <h2>Здравствуйте!</h2>
                    <p>Мы получили запрос на восстановление пароля для вашего аккаунта.</p>
                    <p>Ваш новый временный пароль:</p>
                    
                    <div class="password-box">
                        <h3>Новый пароль:</h3>
                        <div class="password">%s</div>
                    </div>
                    
                    <div class="warning">
                        <strong>⚠️ Важно:</strong>
                        <ul style="margin: 10px 0; padding-left: 20px;">
                            <li>Войдите в систему с этим паролем</li>
                            <li>Сразу же смените пароль на более безопасный</li>
                            <li>Не сообщайте этот пароль никому</li>
                        </ul>
                    </div>
                    
                    <p style="text-align: center;">
                        <a href="#" class="btn">Войти в аккаунт</a>
                    </p>
                </div>
                
                <div class="footer">
                    <p>Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.</p>
                    <p>&copy; 2024 %s. Все права защищены.</p>
                </div>
            </div>
        </body>
        </html>
        """, appName, newPassword, appName);
    }

    /**
     * HTML шаблон для подтверждения email
     */
    private String buildEmailConfirmationTemplate(String confirmationLink) {
        return String.format("""
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Подтверждение email</title>
            <style>
                body {
                    font-family: 'Arial', sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f4f4f4;
                }
                .container {
                    background: #ffffff;
                    padding: 30px;
                    border-radius: 10px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }
                .header {
                    text-align: center;
                    border-bottom: 3px solid #D4AF37;
                    padding-bottom: 20px;
                    margin-bottom: 30px;
                }
                .header h1 {
                    color: #D4AF37;
                    margin: 0;
                    font-size: 28px;
                }
                .content {
                    margin-bottom: 30px;
                }
                .btn {
                    display: inline-block;
                    padding: 15px 30px;
                    background: #D4AF37;
                    color: white;
                    text-decoration: none;
                    border-radius: 5px;
                    font-weight: bold;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn:hover {
                    background: #B8941F;
                }
                .footer {
                    text-align: center;
                    padding-top: 20px;
                    border-top: 1px solid #eee;
                    color: #666;
                    font-size: 14px;
                }
                .link-text {
                    word-break: break-all;
                    background: #f8f9fa;
                    padding: 10px;
                    border-radius: 5px;
                    font-family: monospace;
                    font-size: 12px;
                    color: #666;
                    margin-top: 10px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>📧 %s</h1>
                    <p>Подтверждение email адреса</p>
                </div>
                
                <div class="content">
                    <h2>Подтвердите ваш email</h2>
                    <p>Для завершения восстановления пароля, пожалуйста, подтвердите, что этот email принадлежит вам.</p>
                    
                    <p style="text-align: center;">
                        <a href="%s" class="btn">
                            ✅ Подтвердить email
                        </a>
                    </p>
                    
                    <p><strong>Или скопируйте и вставьте эту ссылку в браузер:</strong></p>
                    <div class="link-text">%s</div>
                    
                    <p style="margin-top: 20px;">
                        <strong>⏰ Срок действия ссылки:</strong> 24 часа
                    </p>
                </div>
                
                <div class="footer">
                    <p>Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.</p>
                    <p>&copy; 2024 %s. Все права защищены.</p>
                </div>
            </div>
        </body>
        </html>
        """, appName, confirmationLink, confirmationLink, appName);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmailTask {
        private String toEmail;
        private String subject;
        private String htmlContent;
        private String type; // "password_reset", "email_confirmation", "html_email"
        private LocalDateTime createdAt;
    }
}