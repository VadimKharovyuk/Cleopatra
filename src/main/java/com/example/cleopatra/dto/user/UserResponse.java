package com.example.cleopatra.dto.user;

import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.enums.ProfileAccessLevel;
import com.example.cleopatra.enums.Role;
import com.example.cleopatra.model.User;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private Role role;
    private Gender gender;

    private String firstName;
    private String lastName;
    private String imageUrl;
    private String imgId;

    private String imgBackground ;
    private String imgBackgroundID;


    private Long followersCount; // количество подписчиков
    private Long followingCount; // количество подписок
    private Long postsCount;

    private String city;

    private LocalDateTime createdAt;


    private Boolean receiveVisitNotifications;


    private Boolean isPrivateProfile ;

    private Boolean isBlocked;

    private LocalDate birthDate;
    private Boolean showBirthday;



    private ProfileAccessLevel profileAccessLevel;
    private ProfileAccessLevel photosAccessLevel;
    private ProfileAccessLevel postsAccessLevel;


    @Size(max = 200, message = "Статус не может быть длиннее 200 символов")
    private String statusPage;


    private Boolean isOnline;
    private Boolean wasOnlineRecently;
    private LocalDateTime lastSeen;
    private String deviceType;
    private String onlineStatusText;

}
