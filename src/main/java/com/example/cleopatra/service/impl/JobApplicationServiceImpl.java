package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.ImageUploadException;
import com.example.cleopatra.ExistsException.JobApplicationSaveException;
import com.example.cleopatra.dto.JobApplication.CreateJobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationDto;
import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.model.JobApplication;
import com.example.cleopatra.repository.JobApplicationRepository;
import com.example.cleopatra.service.ImageValidator;
import com.example.cleopatra.service.JobApplicationService;
import com.example.cleopatra.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationServiceImpl implements JobApplicationService {

    private final StorageService storageService;
    private final JobApplicationRepository jobApplicationRepository;
    private final com.example.cleopatra.mapper.JobApplicationMapper jobApplicationMapper;
    private final ImageValidator imageValidator;

    @Override
    @Transactional
    public JobApplicationDto createJobApplication(CreateJobApplicationDto createDto) {
        log.info("Начинаем создание заявки для пользователя: {}", createDto.getEmail());

        JobApplication jobApplication = prepareJobApplication(createDto);
        processProfilePicture(createDto, jobApplication);
        JobApplication savedApplication = saveJobApplication(jobApplication);

        log.info("Заявка успешно создана с ID: {} для пользователя: {}",
                savedApplication.getId(), createDto.getEmail());

        return jobApplicationMapper.toDto(savedApplication);
    }

    private JobApplication prepareJobApplication(CreateJobApplicationDto createDto) {
        JobApplication jobApplication = jobApplicationMapper.fromCreateDto(createDto);
        jobApplication.setStatus(ApplicationStatus.PENDING);
        return jobApplication;
    }

    private void processProfilePicture(CreateJobApplicationDto createDto, JobApplication jobApplication) {

        MultipartFile profilePicture = createDto.getProfilePicture();
        if (profilePicture == null || profilePicture.isEmpty()) {
            return;
        }

        try {
            // ИСПРАВЛЕНО: сохраняем и URL и ID изображения
            StorageService.StorageResult result = uploadProfilePicture(profilePicture);
            jobApplication.setProfilePictureUrl(result.getUrl());
            jobApplication.setProfilePictureId(result.getImageId());

            log.debug("Изображение профиля успешно загружено для пользователя: {}", createDto.getEmail());
        } catch (Exception e) {
            log.error("Ошибка загрузки изображения для пользователя: {}", createDto.getEmail(), e);
            throw new ImageUploadException("Не удалось загрузить изображение профиля", e);
        }
    }

    private JobApplication saveJobApplication(JobApplication jobApplication) {
        try {
            return jobApplicationRepository.save(jobApplication);
        } catch (Exception e) {
            log.error("Ошибка сохранения заявки в БД для пользователя: {}", jobApplication.getEmail(), e);
            throw new JobApplicationSaveException("Не удалось сохранить заявку в базе данных", e);
        }
    }

    // ИСПРАВЛЕНО: возвращаем StorageResult вместо String
    private StorageService.StorageResult uploadProfilePicture(MultipartFile file) {
        try {
            log.debug("Загрузка изображения профиля. Размер файла: {} bytes", file.getSize());

            // Валидация файла через отдельный валидатор
            imageValidator.validateImage(file);

            // Загрузка через StorageService
            StorageService.StorageResult result = storageService.uploadImage(file);

            log.debug("Изображение успешно загружено. URL: {}, ID: {}",
                    result.getUrl(), result.getImageId());
            return result;

        } catch (IOException e) {
            log.error("Ошибка при загрузке изображения профиля", e);
            throw new ImageUploadException("Не удалось загрузить изображение профиля: " + e.getMessage(), e);
        }
    }
}