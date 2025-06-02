package com.example.cleopatra.dto.user;

import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
}
