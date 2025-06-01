package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.UserRecommendationDto;
import com.example.cleopatra.dto.user.UserRecommendationListDto;

import java.util.List;

public interface RecommendationService {

    List<UserRecommendationDto> getTopRecommendations(Long currentUserId) ;

    UserRecommendationListDto getAllRecommendations(Long currentUserId, int page);


    UserRecommendationListDto searchRecommendations(Long currentUserId, String query, String sort, String followers, String status, int page);
}
