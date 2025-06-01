package com.example.cleopatra.dto.user;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class UserRecommendationListDto {
    private List<UserRecommendationDto> userRecommendations;

    private int currentPage;
    private int itemsPerPage;

    // Для Page
    private Integer totalPages;
    private Long totalItems;

    // Для Slice и общего удобства
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Integer nextPage;
    private Integer previousPage;
}
