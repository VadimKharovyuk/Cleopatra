package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.UserRecommendationDto;
import com.example.cleopatra.dto.user.UserRecommendationListDto;

import java.util.List;

public interface RecommendationService {

    /**
     * Главная страница - топ рекомендации (небольшое количество)
     */
    List<UserRecommendationDto> getTopRecommendations(Long currentUserId);

    /**
     * Поиск/просмотр всех пользователей с пагинацией
     */
    UserRecommendationListDto getAllRecommendations(Long currentUserId, int page);

    /**
     * Поиск пользователей по запросу с пагинацией
     */
    UserRecommendationListDto searchRecommendations(
            Long currentUserId,
            String searchQuery,
            int page);
}
