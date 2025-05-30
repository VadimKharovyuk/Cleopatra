package com.example.cleopatra.dto.JobApplication;

import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.enums.PerformerProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobApplicationFilterDto {

    private PerformerProfile profile;
    private ApplicationStatus status;
    private String country;
    private Integer minAge;
    private Integer maxAge;
    private Integer minExperience;
    private BigDecimal maxBudget;
    private Boolean willingToTravel;
    private String searchQuery;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
    private int page = 0;
    private int size = 20;

}
