package com.example.cleopatra.dto.user;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class UserRecommendationDto {
    private Long id;
    private String imageUrl;

    private String firstName;
    private String lastName;


    // Метрики для рекомендаций
    private Long followersCount;
    private Boolean isFollowing;



}
