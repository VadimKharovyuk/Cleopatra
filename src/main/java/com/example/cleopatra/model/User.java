package com.example.cleopatra.model;

import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.enums.Role;
import jakarta.persistence.*;
import lombok.*;

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


    @Column(name = "followers_count")
    private Long followersCount = 0L;

    @Column(name = "following_count")
    private Long followingCount = 0L;


    @Column(name = "is_private_profile")
    private Boolean isPrivateProfile = false;



    // Добавить в User entity:
    @Column(name = "is_online")
    private Boolean isOnline = false;

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

    @Column(name = "total_visits")
    @Builder.Default
    private Long totalVisits = 0L;





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



    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserOnlineStatus onlineStatus;


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
