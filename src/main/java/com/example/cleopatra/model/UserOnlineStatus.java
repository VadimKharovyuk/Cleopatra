package com.example.cleopatra.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "user_online_status",
        indexes = {
                // Основные индексы для быстрого поиска
                @Index(name = "idx_user_online_status_user_id", columnList = "user_id"),
                @Index(name = "idx_user_online_status_is_online", columnList = "is_online"),
                @Index(name = "idx_user_online_status_last_seen", columnList = "last_seen"),

                // Композитный индекс для сложных запросов (порядок ВАЖЕН!)
                @Index(name = "idx_user_online_status_online_last_seen", columnList = "is_online, last_seen"),

                // Дополнительные полезные индексы
                @Index(name = "idx_user_online_status_device_type", columnList = "device_type"),
                @Index(name = "idx_user_online_status_updated_at", columnList = "updated_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserOnlineStatus {

    @Id
    private Long userId;

    // Связь с пользователем
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // Онлайн ли пользователь
    @Column(name = "is_online")
    private Boolean isOnline = false;

    // Последний раз был онлайн
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    // Устройство с которого подключен
    @Column(name = "device_type", length = 50)
    private String deviceType; // WEB, MOBILE, DESKTOP

    // IP адрес последнего подключения
    @Column(name = "last_ip", length = 45)
    private String lastIp;

    // User Agent браузера/приложения
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Часовой пояс пользователя
    @Column(name = "timezone", length = 50)
    private String timezone;

    // Время создания записи
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Время последнего обновления
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // Методы для удобства
    public boolean wasOnlineRecently() {
        if (isOnline) return true;
        if (lastSeen == null) return false;

        return lastSeen.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    public String getOnlineStatusText() {
        if (isOnline) {
            return "в сети";
        } else if (lastSeen != null) {
            Duration duration = Duration.between(lastSeen, LocalDateTime.now());
            if (duration.toMinutes() < 5) {
                return "недавно";
            } else if (duration.toHours() < 24) {
                return "был(а) в сети " + duration.toHours() + " ч. назад";
            } else {
                return "был(а) в сети " + duration.toDays() + " дн. назад";
            }
        }
        return "давно не был(а) в сети";
    }
}