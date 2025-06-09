package com.example.cleopatra.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String newPassword);

    void sendEmailConfirmation(String toEmail, String confirmationLink);

    // Добавьте этот метод для отправки произвольных HTML писем
    void sendHtmlEmail(String toEmail, String subject, String htmlContent);
}
