package com.example.cleopatra.dto.JobVacancy;

import com.example.cleopatra.enums.PerformerProfile;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobVacancyDto {
    @NotBlank(message = "Название вакансии обязательно")
    private String title;

    @NotBlank(message = "Описание вакансии обязательно")
    private String description;

    @NotNull(message = "Профиль исполнителя обязателен")
    private PerformerProfile requiredProfile;

    private BigDecimal minSalary;

    @NotNull(message = "Минимальный опыт работы обязателен")
    @Min(value = 0, message = "Опыт работы не может быть отрицательным")
    @Builder.Default
    private Integer minWorkExperience = 0;

    @NotBlank(message = "Email для связи обязателен")
    @Email(message = "Некорректный формат email")
    private String contactEmail;

    private String contactPhone;

    @NotBlank(message = "Название компании обязательно")
    private String companyName;

    private String companyWebsite;
    private MultipartFile companyLogo;

    @NotBlank(message = "Страна обязательна")
    private String country;

    @NotBlank(message = "Город обязателен")
    private String city;

    @Min(value = 16, message = "Минимальный возраст 16 лет")
    @Max(value = 100, message = "Максимальный возраст 100 лет")
    @Builder.Default
    private Integer minAge = 18;

    private String requiredSkills;
    private String additionalRequirements;
    private String workConditions;
    private String benefits;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationDeadline;

    @Builder.Default
    private Boolean accommodationProvided = false;

    @Builder.Default
    private Boolean mealsProvided = false;

    @Builder.Default
    private Boolean transportProvided = false;

    private String additionalComments;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isUrgent = false;

    @Builder.Default
    private Boolean isFeatured = false;
}