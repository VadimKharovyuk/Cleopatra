package com.example.cleopatra.model;

import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.enums.ProfileAccessLevel;
import com.example.cleopatra.enums.Role;
import com.example.cleopatra.enums.WallAccessLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender ;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "img_id")
    private String imgId;

    private String imgBackground ;

    private String imgBackgroundID;

    @Column(name = "status_page", length = 200)
    private String statusPage;


    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;


    @Column(name = "show_online_status")
    private Boolean showOnlineStatus = true;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;


    @Column(name = "city")
    private String city;

    @Column(name = "receive_visit_notifications")
    private Boolean receiveVisitNotifications = true;


    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private boolean isBlocked = false;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "blocked_by_admin_id")
    private Long blockedByAdminId;


    @Column(name = "show_birthday", nullable = false)
    private Boolean showBirthday = true;

    @Column(name = "birth_date")
    private LocalDate birthDate;


    @Column(name = "followers_count", nullable = false)
    @Builder.Default  // ← Добавить эту аннотацию
    private Long followersCount = 0L;

    @Column(name = "following_count", nullable = false)
    @Builder.Default  // ← Добавить эту аннотацию
    private Long followingCount = 0L;

    @Column(name = "is_online", nullable = false)
    @Builder.Default  // ← Добавить эту аннотацию
    private Boolean isOnline = false;

    @Column(name = "photo_count", nullable = false)
    @Builder.Default  // ← Добавить эту аннотацию
    private Integer photoCount = 0;

    @Column(name = "welcome_bonus_claimed", nullable = false)
    @Builder.Default  // ← Добавить эту аннотацию
    private Boolean welcomeBonusClaimed = false;


    @Column(name = "total_visits")
    @Builder.Default
    private Long totalVisits = 0L;


    @Enumerated(EnumType.STRING)
    @Column(name = "profile_access_level", nullable = false)
    @Builder.Default
    private ProfileAccessLevel profileAccessLevel = ProfileAccessLevel.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "photos_access_level", nullable = false)
    @Builder.Default
    private ProfileAccessLevel photosAccessLevel = ProfileAccessLevel.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "posts_access_level", nullable = false)
    @Builder.Default
    private ProfileAccessLevel postsAccessLevel = ProfileAccessLevel.PUBLIC;

    @OneToMany(mappedBy = "blockedUser", fetch = FetchType.LAZY)
    @OrderBy("blockedAt DESC")
    @Builder.Default
    private List<SystemBlock> systemBlocks = new ArrayList<>();


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<TrustedDevice> trustedDevices = new ArrayList<>();


    // Подписчики (кто подписался на этого пользователя)
    @OneToMany(mappedBy = "subscribedTo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Subscription> subscribers = new HashSet<>();

    // Подписки (на кого подписан этот пользователь)
    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Subscription> subscriptions = new HashSet<>();


    @OneToMany(mappedBy = "visitedUser", fetch = FetchType.LAZY)
    @OrderBy("visitedAt DESC")
    private List<Visit> pageVisits = new ArrayList<>();

    @OneToMany(mappedBy = "visitor", fetch = FetchType.LAZY)
    @OrderBy("visitedAt DESC")
    private List<Visit> myVisits = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Photo> photos = new ArrayList<>();


    // Посты пользователя
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<Post> posts = new ArrayList<>();



    // Комментарии пользователя
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    // Просмотренные посты
    @OneToMany(mappedBy = "viewer", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PostView> viewedPosts = new HashSet<>();

    // Понравившиеся посты
    @ManyToMany(mappedBy = "likedBy", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Post> likedPosts = new HashSet<>();





    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<ProjectNews> createdNews = new ArrayList<>();


    // Связь с онлайн статусом
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserOnlineStatus onlineStatus;

    // Безопасные методы для получения онлайн статуса
    @Transient
    public boolean isOnline() {
        return onlineStatus != null && Boolean.TRUE.equals(onlineStatus.getIsOnline());
    }

    @Transient
    public LocalDateTime getLastSeen() {
        return onlineStatus != null ? onlineStatus.getLastSeen() : null;
    }

    @Transient
    public String getDeviceType() {
        return onlineStatus != null ? onlineStatus.getDeviceType() : "WEB";
    }

    @Transient
    public boolean wasOnlineRecently() {
        return onlineStatus != null && onlineStatus.wasOnlineRecently();
    }

    @Transient
    public String getOnlineStatusText() {
        return onlineStatus != null ? onlineStatus.getOnlineStatusText() : "статус неизвестен";
    }


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SupportRequest> supportRequests = new ArrayList<>();


    // Пользователи, которых заблокировал этот юзер
    @OneToMany(mappedBy = "blocker", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserBlock> blockedUsers = new HashSet<>();

    // Пользователи, которые заблокировали этого юзера
    @OneToMany(mappedBy = "blocked", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserBlock> blockedByUsers = new HashSet<>();


    // Истории пользователя
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<Story> stories = new ArrayList<>();

    // Просмотренные истории
    @OneToMany(mappedBy = "viewer", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<StoryView> viewedStories = new HashSet<>();


    @Enumerated(EnumType.STRING)
    @Column(name = "wall_access_level")
    @Builder.Default
    private WallAccessLevel wallAccessLevel = WallAccessLevel.PUBLIC;



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

        // Установите значения по умолчанию для полей, которые могут быть null
        if (this.followersCount == null) {
            this.followersCount = 0L;
        }
        if (this.followingCount == null) {
            this.followingCount = 0L;
        }
        if (this.photoCount == null) {
            this.photoCount = 0;
        }
        if (this.totalVisits == null) {
            this.totalVisits = 0L;
        }
        if (this.isOnline == null) {
            this.isOnline = false;
        }
        if (this.showOnlineStatus == null) {
            this.showOnlineStatus = true;
        }
        if (this.receiveVisitNotifications == null) {
            this.receiveVisitNotifications = true;
        }
        if (this.showBirthday == null) {
            this.showBirthday = true;
        }
        if (this.welcomeBonusClaimed == null) {
            this.welcomeBonusClaimed = false;
        }
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }





}
