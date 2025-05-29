package com.example.cleopatra.model;

import com.example.cleopatra.enums.QrAuthStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_auth_sessions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class QrAuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token; // UUID токен

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QrAuthStatus status = QrAuthStatus.PENDING;

    @Column(name = "user_id")
    private Long userId; // ID пользователя после подтверждения (nullable)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = createdAt.plusMinutes(15);
    }



}