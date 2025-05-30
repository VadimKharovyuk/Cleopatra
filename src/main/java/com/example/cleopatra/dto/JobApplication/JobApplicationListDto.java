package com.example.cleopatra.dto.JobApplication;

import com.example.cleopatra.enums.PerformerProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationListDto {
    private List<JobApplicationCardDto> applications;
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