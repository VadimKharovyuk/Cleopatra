package com.example.cleopatra.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "story_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"story_id", "viewer_id"}))
@Builder
public class StoryView {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с историей
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    // Кто просмотрел
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id", nullable = false)
    private User viewer;

    // Время просмотра
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    // Системные поля
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;

        // Автоматически устанавливаем время просмотра
        if (viewedAt == null) {
            viewedAt = now;
        }
    }
}