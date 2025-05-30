package com.example.cleopatra.maper;
import com.example.cleopatra.dto.JobApplication.CreateJobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationCardDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationDto;
import com.example.cleopatra.model.JobApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JobApplicationMapper {

    /**
     * Конвертация CreateDTO в Entity для создания
     */
    public JobApplication fromCreateDto(CreateJobApplicationDto createDto) {
        if (createDto == null) {
            return null;
        }

        return JobApplication.builder()
                // Основная информация
                .name(createDto.getName())
                .fullName(createDto.getFullName())
                .bio(createDto.getBio())

                // Профессиональная информация
                .profile(createDto.getProfile())
                .workExperience(createDto.getWorkExperience())

                // Зарплатные ожидания
                .minSalary(createDto.getMinSalary())
                .maxSalary(createDto.getMaxSalary())
                .currency(StringUtils.hasText(createDto.getCurrency()) ? createDto.getCurrency() : "USD")

                // Контактная информация
                .email(createDto.getEmail())
                .phone(createDto.getPhone())
                .phoneVisible(createDto.getPhoneVisible() != null ? createDto.getPhoneVisible() : true)

                // Социальные сети
                .instagram(createDto.getInstagram())
                .whatsapp(createDto.getWhatsapp())
                .facebook(createDto.getFacebook())

                // Медиа - НЕ мапим файл, только videoUrl
                .videoUrl(createDto.getVideoUrl())

                // Личная информация
                .country(createDto.getCountry())
                .age(createDto.getAge())
                .birthDate(createDto.getBirthDate()) // ИСПРАВЛЕНО: добавлено маппинг birthDate

                // Дополнительная информация
                .additionalSkills(createDto.getAdditionalSkills())
                .availableFrom(createDto.getAvailableFrom()) // ИСПРАВЛЕНО: добавлено маппинг availableFrom
                .willingToTravel(createDto.getWillingToTravel() != null ? createDto.getWillingToTravel() : true)
                .comments(createDto.getComments())

                .build();
    }
    public JobApplicationDto toDto(JobApplication jobApplication) {
        if (jobApplication == null) {
            return null;
        }

        JobApplicationDto dto = new JobApplicationDto();

        // Основная информация
        dto.setName(jobApplication.getName());
        dto.setFullName(jobApplication.getFullName());
        dto.setBio(jobApplication.getBio());

        // Профессиональная информация
        dto.setProfile(jobApplication.getProfile());
        dto.setWorkExperience(jobApplication.getWorkExperience());

        // Зарплатные ожидания
        dto.setMinSalary(jobApplication.getMinSalary());
        dto.setMaxSalary(jobApplication.getMaxSalary());
        dto.setCurrency(jobApplication.getCurrency());

        // Контактная информация
        dto.setEmail(jobApplication.getEmail());
        dto.setPhone(jobApplication.getPhone());
        dto.setPhoneVisible(jobApplication.getPhoneVisible());

        // Социальные сети
        dto.setInstagram(jobApplication.getInstagram());
        dto.setWhatsapp(jobApplication.getWhatsapp());
        dto.setFacebook(jobApplication.getFacebook());

        // Медиа
        dto.setProfilePictureUrl(jobApplication.getProfilePictureUrl());
        dto.setProfilePictureId(jobApplication.getProfilePictureId());
        dto.setVideoUrl(jobApplication.getVideoUrl());

        // Личная информация
        dto.setCountry(jobApplication.getCountry());
        dto.setAge(jobApplication.getAge());
        dto.setBirthDate(jobApplication.getBirthDate());

        // Дополнительная информация
        dto.setAdditionalSkills(jobApplication.getAdditionalSkills());
        dto.setAvailableFrom(jobApplication.getAvailableFrom());
        dto.setWillingToTravel(jobApplication.getWillingToTravel());
        dto.setComments(jobApplication.getComments());

        return dto;
    }
    /**
     * Преобразует List<JobApplication> в List<JobApplicationCardDto>
     */
    public List<JobApplicationCardDto> toCardDtoList(List<JobApplication> applications) {
        if (applications == null) {
            return List.of();
        }

        return applications.stream()
                .map(this::toCardDto)
                .collect(Collectors.toList());
    }

    /**
     * Преобразует JobApplication в JobApplicationCardDto
     */
    public JobApplicationCardDto toCardDto(JobApplication jobApplication) {
        if (jobApplication == null) {
            return null;
        }

        return JobApplicationCardDto.builder()
                .id(jobApplication.getId())
                .name(jobApplication.getName())
                .profile(jobApplication.getProfile())
                .workExperience(jobApplication.getWorkExperience())
                .minSalary(jobApplication.getMinSalary())
                .age(jobApplication.getAge())
                .build();
    }

}