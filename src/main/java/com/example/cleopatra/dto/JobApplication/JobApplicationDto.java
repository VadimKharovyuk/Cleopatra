package com.example.cleopatra.dto.JobApplication;

import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.enums.PerformerProfile;
import com.example.cleopatra.model.JobApplication;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class JobApplicationDto {

    // Основная информация
    @NotBlank(message = "Имя обязательно")
    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    private String name;

    @NotBlank(message = "Полное имя обязательно")
    @Size(max = 100, message = "Полное имя не должно превышать 100 символов")
    private String fullName;

    @Size(max = 2000, message = "Биография не должна превышать 2000 символов")
    private String bio;

    // Профессиональная информация
    @NotNull(message = "Профиль артиста обязателен")
    private PerformerProfile profile;

    @NotNull(message = "Опыт работы обязателен")
    @Min(value = 0, message = "Опыт не может быть отрицательным")
    @Max(value = 50, message = "Опыт не может превышать 50 лет")
    private Integer workExperience;

    // Зарплатные ожидания
    @DecimalMin(value = "0.0", message = "Минимальная зарплата не может быть отрицательной")
    private BigDecimal minSalary;

    @DecimalMin(value = "0.0", message = "Максимальная зарплата не может быть отрицательной")
    private BigDecimal maxSalary;

    private String currency = "USD";

    // Контактная информация
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Size(max = 100, message = "Email не должен превышать 100 символов")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный формат телефона")
    @Size(max = 20, message = "Телефон не должен превышать 20 символов")
    private String phone;

    private Boolean phoneVisible = true;

    // Социальные сети
    @Pattern(regexp = "^@?[A-Za-z0-9_.]*$", message = "Некорректный Instagram username")
    @Size(max = 50, message = "Instagram не должен превышать 50 символов")
    private String instagram;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный номер WhatsApp")
    @Size(max = 20, message = "WhatsApp не должен превышать 20 символов")
    private String whatsapp;

    @Size(max = 100, message = "Facebook не должен превышать 100 символов")
    private String facebook;

    // Медиа
    @Size(max = 500, message = "URL фото не должен превышать 500 символов")
    private String profilePictureUrl;

    @Pattern(regexp = "^(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9_-]+.*$",
            message = "Некорректная ссылка на YouTube")
    @Size(max = 500, message = "Ссылка на видео не должна превышать 500 символов")
    private String videoUrl;

    // Личная информация
    @NotBlank(message = "Страна обязательна")
    @Size(max = 50, message = "Страна не должна превышать 50 символов")
    private String country;

    @NotNull(message = "Возраст обязателен")
    @Min(value = 16, message = "Минимальный возраст 16 лет")
    @Max(value = 80, message = "Максимальный возраст 80 лет")
    private Integer age;

    private LocalDate birthDate;

    // Дополнительная информация
    @Size(max = 1000, message = "Дополнительные навыки не должны превышать 1000 символов")
    private String additionalSkills;

    private LocalDate availableFrom;

    private Boolean willingToTravel = true;

    @Size(max = 1000, message = "Комментарии не должны превышать 1000 символов")
    private String comments;

    // Согласие на обработку данных
    @NotNull(message = "Необходимо согласие на обработку персональных данных")
    private Boolean agreeToDataProcessing;

    @NotNull(message = "Необходимо подтвердить достоверность информации")
    private Boolean confirmDataAccuracy;
}

// DTO для фильтрации заявок (для админ панели)
@Getter
@Setter
class JobApplicationFilterDto {

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

// DTO для ответа со списком заявок
@Getter
@Setter
class JobApplicationListDto {

    private Long id;
    private String name;
    private String fullName;
    private PerformerProfile profile;
    private Integer age;
    private String country;
    private Integer workExperience;
    private String salaryRange;
    private ApplicationStatus status;
    private String email;
    private String phone;
    private Boolean phoneVisible;
    private String profilePictureUrl;
    private String videoUrl;
    private java.time.LocalDateTime createdAt;
    private int daysFromSubmission;



}