package com.example.cleopatra.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PostView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с постом
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // Связь с пользователем, который просмотрел
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User viewer;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;


    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }
}
