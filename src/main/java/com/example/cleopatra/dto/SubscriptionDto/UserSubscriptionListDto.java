package com.example.cleopatra.dto.SubscriptionDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionListDto {
    private List<UserSubscriptionCard> subscriptions ;

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
