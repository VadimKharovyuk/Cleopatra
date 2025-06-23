package com.example.cleopatra.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "forum_comments")
public class ForumComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_id", nullable = false)
    private Forum forum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // Для иерархии комментариев
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ForumComment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ForumComment> children = new ArrayList<>();

    // Для оптимизации отображения
    @Column(name = "depth_level")
    @Builder.Default
    private Integer level = 0;

    @Column(name = "children_count")
    @Builder.Default
    private Integer childrenCount = 0;

    // Мягкое удаление
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

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

        // Устанавливаем уровень вложенности
        if (parent != null) {
            level = parent.getLevel() + 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Вспомогательные методы
    public boolean isReply() {
        return parent != null;
    }

    public boolean hasChildren() {
        return childrenCount > 0;
    }

    public boolean isDeleted() {
        return deleted;
    }
}