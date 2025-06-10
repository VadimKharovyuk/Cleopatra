package com.example.cleopatra.model;

import com.example.cleopatra.enums.StoryEmoji;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "stories")
@Builder
public class Story {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с пользователем
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    // Убери @Lob!
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data", columnDefinition = "bytea")
    private byte[] imageData;


    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "image_id", nullable = false, unique = true)
    private String imageId;

    // Эмодзи реакция
    @Enumerated(EnumType.STRING)
    @Column(name = "emoji")
    private StoryEmoji emoji;

    // Описание
    @Column(name = "description", length = 500)
    private String description;

    // Время истечения (24 часа)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Просмотры истории
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("viewedAt DESC")
    @Builder.Default
    private List<StoryView> views = new ArrayList<>();

    // Счетчик просмотров для производительности
    @Column(name = "views_count")
    @Builder.Default
    private Long viewsCount = 0L;

    // Системные поля
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        // Автоматически устанавливаем время истечения через 24 часа
        if (expiresAt == null) {
            expiresAt = now.plusHours(24);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Проверка истечения
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Увеличение счетчика просмотров
    public void incrementViewsCount() {
        this.viewsCount = (this.viewsCount == null ? 0L : this.viewsCount) + 1;
    }
}