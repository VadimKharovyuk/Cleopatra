package com.example.cleopatra.model;

import com.example.cleopatra.enums.NewsType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "project_news")
@Builder
public class ProjectNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "photo_id")
    private String photoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "news_type", nullable = false)
    private NewsType newsType;

    // Добавить статус публикации
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // Счетчики
    @Column(name = "views_count", nullable = false)
    @Builder.Default
    private Long viewsCount = 0L;

    // Временные метки
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        // Устанавливаем publishedAt только если новость опубликована
        if (Boolean.TRUE.equals(isPublished)) {
            publishedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Устанавливаем publishedAt только при первой публикации
        if (Boolean.TRUE.equals(isPublished) && publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }

    // Удобный метод для публикации новости
    public void publish() {
        this.isPublished = true;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    // Удобный метод для снятия с публикации
    public void unpublish() {
        this.isPublished = false;
    }

    // Проверка, опубликована ли новость
    public boolean isPublished() {
        return Boolean.TRUE.equals(isPublished);
    }
}