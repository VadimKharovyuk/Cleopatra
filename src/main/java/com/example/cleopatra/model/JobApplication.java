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

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String fullName;

    private String bio;

    // Профессиональная информация
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PerformerProfile profile;

    private Integer workExperience;

    private BigDecimal minSalary;

    private BigDecimal maxSalary;

    @Builder.Default
    private String currency = "USD";

    @Column(nullable = false, length = 100)
    private String email;

    private String phone;

    @Builder.Default
    @Column(name = "phone_visible")
    private Boolean phoneVisible = true;

    // Социальные сети
    @Column(length = 50)
    private String instagram;

    @Column(length = 20)
    private String whatsapp;

    @Column(length = 100)
    private String facebook;

    // Медиа файлы - ИСПРАВЛЕНО: правильные названия полей
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "profile_picture_id", length = 100)
    private String profilePictureId;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    // Личная информация
    @Column(nullable = false, length = 50)
    private String country;

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
    @Column(name = "additional_skills", columnDefinition = "TEXT")
    private String additionalSkills;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Builder.Default
    @Column(name = "willing_to_travel")
    private Boolean willingToTravel = true;

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
    private Long reviewedBy;
}