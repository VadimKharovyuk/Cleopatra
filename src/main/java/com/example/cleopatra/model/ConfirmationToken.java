package com.example.cleopatra.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "confirmation_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType tokenType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    public enum TokenType {
        EMAIL_CONFIRMATION,
        PASSWORD_RESET
    }

    // Конструктор для создания нового токена
    public ConfirmationToken(String token, String email, Long userId, TokenType tokenType, int hoursToExpire) {
        this.token = token;
        this.email = email;
        this.userId = userId;
        this.tokenType = tokenType;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(hoursToExpire);
        this.isUsed = false;
    }

    // Методы для проверки статуса токена
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isUsed && !isExpired() && confirmedAt == null;
    }

    public void markAsUsed() {
        this.isUsed = true;
        this.confirmedAt = LocalDateTime.now();
    }
}