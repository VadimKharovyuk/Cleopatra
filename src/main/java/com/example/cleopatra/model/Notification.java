package com.example.cleopatra.model;

import com.example.cleopatra.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Кому отправляется уведомление
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // Кто инициировал действие (может быть null для системных уведомлений)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    // Тип уведомления
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    // Заголовок уведомления
    @Column(name = "title", nullable = false)
    private String title;

    // Текст уведомления
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    // Дополнительные данные в JSON формате
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    // ID связанной сущности (профиль, пост, комментарий и т.д.)
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    // Тип связанной сущности
    @Column(name = "related_entity_type")
    private String relatedEntityType;

    // Статусы
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
