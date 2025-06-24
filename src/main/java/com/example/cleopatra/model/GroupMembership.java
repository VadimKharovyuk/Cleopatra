package com.example.cleopatra.model;


import com.example.cleopatra.enums.GroupRole;
import com.example.cleopatra.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "group_memberships")
@Builder
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private GroupRole role = GroupRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.PENDING;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "ban_reason")
    private String banReason;

    @Column(name = "banned_by_user_id")
    private Long bannedByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_user_id", insertable = false, updatable = false)
    private User bannedByUser;

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

        if (this.requestedAt == null) {
            this.requestedAt = now;
        }
        if (this.role == null) {
            this.role = GroupRole.MEMBER;
        }
        if (this.status == null) {
            this.status = MembershipStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Вспомогательные методы
    @Transient
    public boolean isOwner() {
        return GroupRole.OWNER.equals(this.role);
    }

    @Transient
    public boolean isAdmin() {
        return GroupRole.ADMIN.equals(this.role);
    }

    @Transient
    public boolean isModerator() {
        return GroupRole.MODERATOR.equals(this.role);
    }

    @Transient
    public boolean isApproved() {
        return MembershipStatus.APPROVED.equals(this.status);
    }

    @Transient
    public boolean isPending() {
        return MembershipStatus.PENDING.equals(this.status);
    }

    @Transient
    public boolean isBanned() {
        return MembershipStatus.BANNED.equals(this.status);
    }

    @Transient
    public boolean hasAdminRights() {
        return isOwner() || isAdmin();
    }

    @Transient
    public boolean hasModeratorRights() {
        return isOwner() || isAdmin() || isModerator();
    }
}
