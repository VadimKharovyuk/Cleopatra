package com.example.cleopatra.dto.JobVacancy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobVacancyListDto {
    private List<JobVacancyCardDto> vacancies;
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