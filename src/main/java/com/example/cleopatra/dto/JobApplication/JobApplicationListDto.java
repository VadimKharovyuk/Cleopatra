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
    private int totalPages;
    private int currentPage;
    private long totalItems;
    private int itemsPerPage;
}