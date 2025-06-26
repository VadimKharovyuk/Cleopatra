package com.example.cleopatra.model;

import com.example.cleopatra.enums.*;
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
@Table(name = "groups")
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "img_id")
    private String imgId;

    @Column(name = "background_image_url")
    private String backgroundImageUrl;

    @Column(name = "background_img_id")
    private String backgroundImgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false)
    @Builder.Default
    private GroupType groupType = GroupType.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_status", nullable = false)
    @Builder.Default
    private GroupStatus groupStatus = GroupStatus.ACTIVE;

    @Column(name = "member_count", nullable = false)
    @Builder.Default
    private Long memberCount = 0L;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("joinedAt DESC")
    @Builder.Default
    private List<GroupMembership> memberships = new ArrayList<>();

    @Column(name = "post_count")
    @Builder.Default
    private Long postCount = 0L;


    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<GroupPost> posts = new ArrayList<>();

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

        if (this.memberCount == null) {
            this.memberCount = 0L;
        }
        if (this.postCount == null) {
            this.postCount = 0L;
        }
        if (this.groupType == null) {
            this.groupType = GroupType.OPEN;
        }
        if (this.groupStatus == null) {
            this.groupStatus = GroupStatus.ACTIVE;
        }
    }



    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Вспомогательные методы
    @Transient
    public boolean isOpen() {
        return GroupType.OPEN.equals(this.groupType);
    }

    @Transient
    public boolean isClosed() {
        return GroupType.CLOSED.equals(this.groupType);
    }

    @Transient
    public boolean isPrivate() {
        return GroupType.PRIVATE.equals(this.groupType);
    }

    @Transient
    public boolean isActive() {
        return GroupStatus.ACTIVE.equals(this.groupStatus);
    }
}