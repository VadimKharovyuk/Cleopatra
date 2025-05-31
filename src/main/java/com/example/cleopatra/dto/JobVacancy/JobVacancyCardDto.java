package com.example.cleopatra.dto.JobVacancy;
import com.example.cleopatra.enums.PerformerProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobVacancyCardDto {
    private Long id;
    private String title;
    private PerformerProfile requiredProfile;
    private String companyLogoUrl;
    private String country;
    private String city;
    private BigDecimal minSalary;
    private String currency;
    private Integer applicationsCount;
    private Boolean isUrgent;
    private Boolean isFeatured;
}
