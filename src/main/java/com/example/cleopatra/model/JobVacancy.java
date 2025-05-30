package com.example.cleopatra.model;
import com.example.cleopatra.enums.PerformerProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_vacancies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobVacancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Основная информация о вакансии
    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // Профессиональные требования
    @Enumerated(EnumType.STRING)
    @Column(name = "required_profile", nullable = false, length = 30)
    private PerformerProfile requiredProfile;

    @Column(name = "min_work_experience", nullable = false)
    private Integer minWorkExperience;


    // Зарплатные условия
    @Column(name = "min_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal minSalary;


    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    // Контактная информация работодателя
    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "company_website", length = 500)
    private String companyWebsite;

    // Социальные сети компании
    @Column(name = "company_instagram", length = 50)
    private String companyInstagram;

    @Column(name = "company_facebook", length = 100)
    private String companyFacebook;

    // Медиа файлы
    @Column(name = "company_logo_url", length = 500)
    private String companyLogoUrl;

    @Column(name = "company_logo_id", length = 100)
    private String companyLogoId;


    // Локация и условия работы
    @Column(nullable = false, length = 50)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(length = 200)
    private String address;

    // Возрастные требования
    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    // Конкретные профессиональные навыки (танцы, вокал, инструменты, языки и тд)
    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    // Специфические требования не связанные с навыками (мед.справка, внешность, график и тд)
    @Column(name = "additional_requirements", columnDefinition = "TEXT")
    private String additionalRequirements;

    // Как организован рабочий процесс (график, место работы, командировки, репетиции и тд)
    @Column(name = "work_conditions", columnDefinition = "TEXT")
    private String workConditions;

    // Что предлагаем сверх зарплаты (питание, такси, страховка, бонусы, отпуск и тд)
    @Column(columnDefinition = "TEXT")
    private String benefits;

    // Временные параметры
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "application_deadline", nullable = false)
    private LocalDate applicationDeadline;

    // Условия работы
    @Column(name = "requires_travel")
    @Builder.Default
    private Boolean requiresTravel = false;

    @Column(name = "accommodation_provided")
    @Builder.Default
    private Boolean accommodationProvided = false;

    @Column(name = "meals_provided")
    @Builder.Default
    private Boolean mealsProvided = false;

    @Column(name = "transport_provided")
    @Builder.Default
    private Boolean transportProvided = false;



    @Column(name = "additional_comments", columnDefinition = "TEXT")
    private String additionalComments;

    // Настройки публикации
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_urgent")
    @Builder.Default
    private Boolean isUrgent = false;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;


    // Системные поля
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_by")
    private Long createdBy; // ID пользователя, создавшего вакансию

    @Column(name = "updated_by")
    private Long updatedBy; // ID пользователя, обновившего вакансию

    // Статистика
    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    @Column(name = "applications_count")
    @Builder.Default
    private Integer applicationsCount = 0;


}