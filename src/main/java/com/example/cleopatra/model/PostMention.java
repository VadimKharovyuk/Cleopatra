package com.example.cleopatra.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_mentions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PostMention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentioned_user_id", nullable = false)
    private User mentionedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentioned_by_user_id", nullable = false)
    private User mentionedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
