package com.example.cleopatra.maper;

import com.example.cleopatra.dto.JobApplication.JobApplicationDto;
import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.model.JobApplication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class JobApplicationMapper {

    /**
     * Преобразует DTO в Entity для создания новой заявки
     */
    public JobApplication toEntity(JobApplicationDto dto) {
        if (dto == null) {
            return null;
        }

        return JobApplication.builder()
                // Основная информация
                .name(dto.getName())
                .fullName(dto.getFullName())
                .bio(dto.getBio())

                // Профессиональная информация
                .profile(dto.getProfile())
                .workExperience(dto.getWorkExperience())

                // Зарплатные ожидания
                .minSalary(dto.getMinSalary())
                .maxSalary(dto.getMaxSalary())
                .currency(dto.getCurrency())

                // Контактная информация
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .phoneVisible(dto.getPhoneVisible())

                // Социальные сети
                .instagram(dto.getInstagram())
                .whatsapp(dto.getWhatsapp())
                .facebook(dto.getFacebook())

                // Медиа
                .profilePictureUrl(dto.getProfilePictureUrl())
                .videoUrl(dto.getVideoUrl())

                // Личная информация
                .country(dto.getCountry())
                .age(dto.getAge())
                .birthDate(dto.getBirthDate())

                // Дополнительная информация
                .additionalSkills(dto.getAdditionalSkills())
                .availableFrom(dto.getAvailableFrom())
                .willingToTravel(dto.getWillingToTravel())
                .comments(dto.getComments())

                // Системные поля устанавливаются автоматически
                .status(ApplicationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Преобразует Entity в DTO для редактирования
     */
    public JobApplicationDto toDto(JobApplication entity) {
        if (entity == null) {
            return null;
        }

        JobApplicationDto dto = new JobApplicationDto();

        // Основная информация
        dto.setName(entity.getName());
        dto.setFullName(entity.getFullName());
        dto.setBio(entity.getBio());

        // Профессиональная информация
        dto.setProfile(entity.getProfile());
        dto.setWorkExperience(entity.getWorkExperience());

        // Зарплатные ожидания
        dto.setMinSalary(entity.getMinSalary());
        dto.setMaxSalary(entity.getMaxSalary());
        dto.setCurrency(entity.getCurrency());

        // Контактная информация
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setPhoneVisible(entity.getPhoneVisible());

        // Социальные сети
        dto.setInstagram(entity.getInstagram());
        dto.setWhatsapp(entity.getWhatsapp());
        dto.setFacebook(entity.getFacebook());

        // Медиа
        dto.setProfilePictureUrl(entity.getProfilePictureUrl());
        dto.setVideoUrl(entity.getVideoUrl());

        // Личная информация
        dto.setCountry(entity.getCountry());
        dto.setAge(entity.getAge());
        dto.setBirthDate(entity.getBirthDate());

        // Дополнительная информация
        dto.setAdditionalSkills(entity.getAdditionalSkills());
        dto.setAvailableFrom(entity.getAvailableFrom());
        dto.setWillingToTravel(entity.getWillingToTravel());
        dto.setComments(entity.getComments());

        // Поля согласия не заполняем при редактировании
        dto.setAgreeToDataProcessing(true);
        dto.setConfirmDataAccuracy(true);

        return dto;
    }

    /**
     * Обновляет существующую Entity данными из DTO
     */
    public void updateEntity(JobApplication entity, JobApplicationDto dto) {
        if (entity == null || dto == null) {
            return;
        }

        // Основная информация
        entity.setName(dto.getName());
        entity.setFullName(dto.getFullName());
        entity.setBio(dto.getBio());

        // Профессиональная информация
        entity.setProfile(dto.getProfile());
        entity.setWorkExperience(dto.getWorkExperience());

        // Зарплатные ожидания
        entity.setMinSalary(dto.getMinSalary());
        entity.setMaxSalary(dto.getMaxSalary());
        entity.setCurrency(dto.getCurrency());

        // Контактная информация
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setPhoneVisible(dto.getPhoneVisible());

        // Социальные сети
        entity.setInstagram(dto.getInstagram());
        entity.setWhatsapp(dto.getWhatsapp());
        entity.setFacebook(dto.getFacebook());

        // Медиа
        entity.setProfilePictureUrl(dto.getProfilePictureUrl());
        entity.setVideoUrl(dto.getVideoUrl());

        // Личная информация
        entity.setCountry(dto.getCountry());
        entity.setAge(dto.getAge());
        entity.setBirthDate(dto.getBirthDate());

        // Дополнительная информация
        entity.setAdditionalSkills(dto.getAdditionalSkills());
        entity.setAvailableFrom(dto.getAvailableFrom());
        entity.setWillingToTravel(dto.getWillingToTravel());
        entity.setComments(dto.getComments());

        // Системные поля обновляются автоматически через @UpdateTimestamp
    }




}