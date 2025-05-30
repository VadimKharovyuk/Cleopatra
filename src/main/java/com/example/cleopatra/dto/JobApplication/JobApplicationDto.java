package com.example.cleopatra.dto.JobApplication;

import com.example.cleopatra.enums.PerformerProfile;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class JobApplicationDto {

    // Основная информация
    private String name;
    private String fullName;
    private String bio;

    // Профессиональная информация
    private PerformerProfile profile;
    private Integer workExperience;

    // Зарплатные ожидания
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency;

    // Контактная информация
    private String email;
    private String phone;
    private Boolean phoneVisible;

    // Социальные сети
    private String instagram;
    private String whatsapp;
    private String facebook;


    private String profilePictureUrl;
    private String profilePictureId;
    private String videoUrl;

    // Личная информация
    private String country;
    private Integer age;
    private LocalDate birthDate;

    // Дополнительная информация
    private String additionalSkills;
    private LocalDate availableFrom;
    private Boolean willingToTravel;
    private String comments;


}