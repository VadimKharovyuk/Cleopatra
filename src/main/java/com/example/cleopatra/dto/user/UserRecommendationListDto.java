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
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Integer nextPage;
    private Integer previousPage;
}
