package com.example.cleopatra.dto.JobVacancy;

import com.example.cleopatra.enums.PerformerProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobVacancyDto {
    private Long id;

    private String title;
    private String description;
    private PerformerProfile requiredProfile;
    private Integer minWorkExperience;
    private BigDecimal minSalary;
    private String currency;
    private String contactEmail;
    private String contactPhone;
    private String companyName;
    private String companyWebsite;
    private String companyInstagram;
    private String companyFacebook;
    private String companyLogoUrl;
    private String companyLogoId;
    private String country;
    private String city;
    private String address;
    private Integer minAge;
    private Integer maxAge;
    private String requiredSkills;
    private String additionalRequirements;
    private String workConditions;
    private String benefits;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationDeadline;
    private Boolean requiresTravel;
    private Boolean accommodationProvided;
    private Boolean mealsProvided;
    private Boolean transportProvided;
    private String additionalComments;
    private Boolean isActive;
    private Boolean isUrgent;
    private Boolean isFeatured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private Long createdBy;
    private Long updatedBy;
    private Integer viewsCount;
    private Integer applicationsCount;
}
