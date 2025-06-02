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

    @Column(name = "posts_count")
    private Long postsCount = 0L;


    @Column(name = "city")
    private String city;


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
