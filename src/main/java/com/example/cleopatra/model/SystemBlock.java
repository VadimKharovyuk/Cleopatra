package com.example.cleopatra.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "system_blocks")
@Builder
public class SystemBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blockedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_admin_id", nullable = false)
    private User blockedByAdmin;

    @Column(name = "block_reason", nullable = false)
    private String blockReason;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    @Column(name = "unblocked_at")
    private LocalDateTime unblockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unblocked_by_admin_id")
    private User unblockedByAdmin;

    @Column(name = "unblock_reason")
    private String unblockReason;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (blockedAt == null) {
            blockedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}