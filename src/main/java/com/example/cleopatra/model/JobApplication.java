package com.example.cleopatra.model;

import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.enums.PerformerProfile;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "job_applications", indexes = {
        @Index(name = "idx_profile", columnList = "profile"),
        @Index(name = "idx_country", columnList = "country"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_salary_range", columnList = "min_salary, max_salary")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Основная информация
    @NotBlank(message = "Имя обязательно")
    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    @Column(nullable = false, length = 50)
    private String name;

    @NotBlank(message = "Полное имя обязательно")
    @Size(max = 100, message = "Полное имя не должно превышать 100 символов")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Size(max = 2000, message = "Биография не должна превышать 2000 символов")
    @Column(columnDefinition = "TEXT")
    private String bio;

    // Профессиональная информация
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Профиль артиста обязателен")
    @Column(nullable = false, length = 30)
    private PerformerProfile profile;

    @NotNull(message = "Опыт работы обязателен")
    @Min(value = 0, message = "Опыт не может быть отрицательным")
    @Max(value = 50, message = "Опыт не может превышать 50 лет")
    @Column(name = "work_experience", nullable = false)
    private Integer workExperience; // в годах

    // Зарплатные ожидания
    @DecimalMin(value = "0.0", message = "Минимальная зарплата не может быть отрицательной")
    @Column(name = "min_salary", precision = 10, scale = 2)
    private BigDecimal minSalary;

    @DecimalMin(value = "0.0", message = "Максимальная зарплата не может быть отрицательной")
    @Column(name = "max_salary", precision = 10, scale = 2)
    private BigDecimal maxSalary;

    @Size(max = 3, message = "Код валюты должен быть 3 символа")
    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    // Контактная информация
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Size(max = 100, message = "Email не должен превышать 100 символов")
    @Column(nullable = false, length = 100)
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный формат телефона")
    @Size(max = 20, message = "Телефон не должен превышать 20 символов")
    @Column(length = 20)
    private String phone;

    @Builder.Default
    @Column(name = "phone_visible")
    private Boolean phoneVisible = true; // показывать/скрывать телефон

    // Социальные сети
    @Pattern(regexp = "^@?[A-Za-z0-9_.]+$", message = "Некорректный Instagram username")
    @Size(max = 50, message = "Instagram не должен превышать 50 символов")
    @Column(length = 50)
    private String instagram;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный номер WhatsApp")
    @Size(max = 20, message = "WhatsApp не должен превышать 20 символов")
    @Column(length = 20)
    private String whatsapp;

    @Size(max = 100, message = "Facebook не должен превышать 100 символов")
    @Column(length = 100)
    private String facebook;

    // Медиа файлы
    @Size(max = 500, message = "URL фото не должен превышать 500 символов")
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Pattern(regexp = "^(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9_-]+.*$",
            message = "Некорректная ссылка на YouTube")
    @Size(max = 500, message = "Ссылка на видео не должна превышать 500 символов")
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    // Личная информация
    @NotBlank(message = "Страна обязательна")
    @Size(max = 50, message = "Страна не должна превышать 50 символов")
    @Column(nullable = false, length = 50)
    private String country;

    @NotNull(message = "Возраст обязателен")
    @Min(value = 16, message = "Минимальный возраст 16 лет")
    @Max(value = 80, message = "Максимальный возраст 80 лет")
    @Column(nullable = false)
    private Integer age;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    // Статус заявки
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    // Дополнительная информация
    @Size(max = 1000, message = "Дополнительные навыки не должны превышать 1000 символов")
    @Column(name = "additional_skills", columnDefinition = "TEXT")
    private String additionalSkills;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Builder.Default
    @Column(name = "willing_to_travel")
    private Boolean willingToTravel = true;

    @Size(max = 1000, message = "Комментарии не должны превышать 1000 символов")
    @Column(columnDefinition = "TEXT")
    private String comments;

    // Системные поля
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy; // ID администратора, который рассмотрел заявку

    // Методы для удобства

    public String getFullProfileName() {
        return profile != null ? profile.getDisplayNameWithIcon() : "Не указан";
    }

    public String getSalaryRange() {
        if (minSalary == null && maxSalary == null) {
            return "Не указана";
        }
        if (minSalary != null && maxSalary != null) {
            return String.format("%s - %s %s", minSalary, maxSalary, currency);
        }
        if (minSalary != null) {
            return String.format("от %s %s", minSalary, currency);
        }
        return String.format("до %s %s", maxSalary, currency);
    }

    public String getContactInfo() {
        StringBuilder contacts = new StringBuilder();
        contacts.append("📧 ").append(email);

        if (phoneVisible && phone != null) {
            contacts.append(" | 📱 ").append(phone);
        }

        if (instagram != null) {
            contacts.append(" | 📷 @").append(instagram);
        }

        if (whatsapp != null) {
            contacts.append(" | 💬 ").append(whatsapp);
        }

        return contacts.toString();
    }

    public boolean isReviewed() {
        return status != ApplicationStatus.PENDING;
    }

    public int getDaysFromSubmission() {
        return (int) java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
    }
}