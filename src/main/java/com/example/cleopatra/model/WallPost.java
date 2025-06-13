package com.example.cleopatra.model;

import com.example.cleopatra.enums.PostVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "wall_posts")
@Builder
public class WallPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Текст поста на стене
    @Column(columnDefinition = "TEXT")
    private String text;

    // URL картинки (если есть)
    @Column(name = "pic_url")
    private String picUrl;

    // ID картинки для удаления из облака
    @Column(name = "pic_id")
    private String picId;

    // Владелец стены (на чьей стене размещен пост)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wall_owner_id", nullable = false)
    private User wallOwner;

    // Автор поста (кто написал)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;



    // Лайки
    @Column(name = "likes_count")
    @Builder.Default
    private Long likesCount = 0L;

    // Комментарии (счетчик)
    @Column(name = "comments_count")
    @Builder.Default
    private Long commentsCount = 0L;

    @Column(name = "is_edited")
    @Builder.Default
    private Boolean isEdited = false;


    // В WallPost добавить:
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}