package com.example.cleopatra.maper;

import com.example.cleopatra.dto.JobVacancy.CreateJobVacancyDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyCardDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyDto;
import com.example.cleopatra.model.JobVacancy;

import org.springframework.stereotype.Component;

@Component
public class JobVacancyMapper {

    public JobVacancy toEntity(CreateJobVacancyDto createJobVacancyDto) {
        if (createJobVacancyDto == null) {
            return null;
        }

        return JobVacancy.builder()
                .title(createJobVacancyDto.getTitle())
                .description(createJobVacancyDto.getDescription())
                .requiredProfile(createJobVacancyDto.getRequiredProfile())
                .minSalary(createJobVacancyDto.getMinSalary())
                .contactEmail(createJobVacancyDto.getContactEmail())
                .contactPhone(createJobVacancyDto.getContactPhone())
                .companyName(createJobVacancyDto.getCompanyName())
                .companyWebsite(createJobVacancyDto.getCompanyWebsite())
                .country(createJobVacancyDto.getCountry())
                .city(createJobVacancyDto.getCity())
                // Обеспечиваем значения по умолчанию
                .minWorkExperience(createJobVacancyDto.getMinWorkExperience() != null ?
                        createJobVacancyDto.getMinWorkExperience() : 0)
                .minAge(createJobVacancyDto.getMinAge() != null ?
                        createJobVacancyDto.getMinAge() : 18)
                .requiredSkills(createJobVacancyDto.getRequiredSkills())
                .additionalRequirements(createJobVacancyDto.getAdditionalRequirements())
                .workConditions(createJobVacancyDto.getWorkConditions())
                .benefits(createJobVacancyDto.getBenefits())
                .startDate(createJobVacancyDto.getStartDate())
                .endDate(createJobVacancyDto.getEndDate())
                .applicationDeadline(createJobVacancyDto.getApplicationDeadline())
                .accommodationProvided(Boolean.TRUE.equals(createJobVacancyDto.getAccommodationProvided()))
                .mealsProvided(Boolean.TRUE.equals(createJobVacancyDto.getMealsProvided()))
                .transportProvided(Boolean.TRUE.equals(createJobVacancyDto.getTransportProvided()))
                .additionalComments(createJobVacancyDto.getAdditionalComments())
                .isActive(createJobVacancyDto.getIsActive() != null ?
                        createJobVacancyDto.getIsActive() : true)
                .isUrgent(Boolean.TRUE.equals(createJobVacancyDto.getIsUrgent()))
                .isFeatured(Boolean.TRUE.equals(createJobVacancyDto.getIsFeatured()))
                .build();
    }

    public JobVacancyDto toDto(JobVacancy savedVacancy) {
        if (savedVacancy == null) {
            return null;
        }

        return JobVacancyDto.builder()
                .id(savedVacancy.getId())
                .title(savedVacancy.getTitle())
                .description(savedVacancy.getDescription())
                .requiredProfile(savedVacancy.getRequiredProfile())
                .minWorkExperience(savedVacancy.getMinWorkExperience())
                .minSalary(savedVacancy.getMinSalary())
                .currency(savedVacancy.getCurrency())
                .contactEmail(savedVacancy.getContactEmail())
                .contactPhone(savedVacancy.getContactPhone())
                .companyName(savedVacancy.getCompanyName())
                .companyWebsite(savedVacancy.getCompanyWebsite())
                .companyInstagram(savedVacancy.getCompanyInstagram())
                .companyFacebook(savedVacancy.getCompanyFacebook())
                .companyLogoUrl(savedVacancy.getCompanyLogoUrl())
                .companyLogoId(savedVacancy.getCompanyLogoId())
                .country(savedVacancy.getCountry())
                .city(savedVacancy.getCity())
                .address(savedVacancy.getAddress())
                .minAge(savedVacancy.getMinAge())
                .maxAge(savedVacancy.getMaxAge())
                .requiredSkills(savedVacancy.getRequiredSkills())
                .additionalRequirements(savedVacancy.getAdditionalRequirements())
                .workConditions(savedVacancy.getWorkConditions())
                .benefits(savedVacancy.getBenefits())
                .startDate(savedVacancy.getStartDate())
                .endDate(savedVacancy.getEndDate())
                .applicationDeadline(savedVacancy.getApplicationDeadline())
                .requiresTravel(savedVacancy.getRequiresTravel())
                .accommodationProvided(savedVacancy.getAccommodationProvided())
                .mealsProvided(savedVacancy.getMealsProvided())
                .transportProvided(savedVacancy.getTransportProvided())
                .additionalComments(savedVacancy.getAdditionalComments())
                .isActive(savedVacancy.getIsActive())
                .isUrgent(savedVacancy.getIsUrgent())
                .isFeatured(savedVacancy.getIsFeatured())
                .createdAt(savedVacancy.getCreatedAt())
                .updatedAt(savedVacancy.getUpdatedAt())
                .publishedAt(savedVacancy.getPublishedAt())
                .closedAt(savedVacancy.getClosedAt())
                .createdBy(savedVacancy.getCreatedBy())
                .updatedBy(savedVacancy.getUpdatedBy())
                .viewsCount(savedVacancy.getViewsCount())
                .applicationsCount(savedVacancy.getApplicationsCount())
                .build();
    }

    public JobVacancyCardDto toCardDto(JobVacancy jobVacancy) {
        if (jobVacancy == null) {
            return null;
        }

        return JobVacancyCardDto.builder()
                .id(jobVacancy.getId())
                .title(jobVacancy.getTitle())
                .requiredProfile(jobVacancy.getRequiredProfile())
                .companyLogoUrl(jobVacancy.getCompanyLogoUrl())
                .country(jobVacancy.getCountry())
                .city(jobVacancy.getCity())
                .minSalary(jobVacancy.getMinSalary())
                .currency(jobVacancy.getCurrency())
                .applicationsCount(jobVacancy.getApplicationsCount())
                .isUrgent(jobVacancy.getIsUrgent())
                .isFeatured(jobVacancy.getIsFeatured())
                .build();
    }

    public void updateEntityFromDto(JobVacancyDto dto, JobVacancy entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setRequiredProfile(dto.getRequiredProfile());
        entity.setMinWorkExperience(dto.getMinWorkExperience());
        entity.setMinSalary(dto.getMinSalary());
        entity.setCurrency(dto.getCurrency());
        entity.setContactEmail(dto.getContactEmail());
        entity.setContactPhone(dto.getContactPhone());
        entity.setCompanyName(dto.getCompanyName());
        entity.setCompanyWebsite(dto.getCompanyWebsite());
        entity.setCompanyInstagram(dto.getCompanyInstagram());
        entity.setCompanyFacebook(dto.getCompanyFacebook());
        entity.setCountry(dto.getCountry());
        entity.setCity(dto.getCity());
        entity.setAddress(dto.getAddress());
        entity.setMinAge(dto.getMinAge());
        entity.setMaxAge(dto.getMaxAge());
        entity.setRequiredSkills(dto.getRequiredSkills());
        entity.setAdditionalRequirements(dto.getAdditionalRequirements());
        entity.setWorkConditions(dto.getWorkConditions());
        entity.setBenefits(dto.getBenefits());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setApplicationDeadline(dto.getApplicationDeadline());
        entity.setRequiresTravel(dto.getRequiresTravel());
        entity.setAccommodationProvided(dto.getAccommodationProvided());
        entity.setMealsProvided(dto.getMealsProvided());
        entity.setTransportProvided(dto.getTransportProvided());
        entity.setAdditionalComments(dto.getAdditionalComments());
        entity.setIsActive(dto.getIsActive());
        entity.setIsUrgent(dto.getIsUrgent());
        entity.setIsFeatured(dto.getIsFeatured());
        entity.setMinWorkExperience(dto.getMinWorkExperience());
    }
}