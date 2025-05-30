package com.example.cleopatra.service.impl;
import com.example.cleopatra.ExistsException.ImageUploadException;
import com.example.cleopatra.ExistsException.JobApplicationSaveException;
import com.example.cleopatra.dto.JobApplication.CreateJobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationCardDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationListDto;
import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.enums.PerformerProfile;
import com.example.cleopatra.maper.JobApplicationMapper;
import com.example.cleopatra.model.JobApplication;
import com.example.cleopatra.repository.JobApplicationRepository;
import com.example.cleopatra.service.ImageValidator;
import com.example.cleopatra.service.JobApplicationService;
import com.example.cleopatra.service.StorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationServiceImpl implements JobApplicationService {

    private final StorageService storageService;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationMapper jobApplicationMapper;
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

    @Override
    public JobApplicationListDto getAllApplications(Pageable pageable) {
        Page<JobApplication> jobApplications = jobApplicationRepository.findAll(pageable);

        List<JobApplicationCardDto> applicationCards = jobApplications.getContent()
                .stream()
                .map(jobApplicationMapper::toCardDto)
                .toList();

        int currentPage = jobApplications.getNumber();
        int totalPages = jobApplications.getTotalPages();

        return JobApplicationListDto.builder()
                .applications(applicationCards)
                .currentPage(currentPage)
                .itemsPerPage(jobApplications.getSize())
                .totalPages(totalPages)
                .totalItems(jobApplications.getTotalElements())
                .hasNext(jobApplications.hasNext())
                .hasPrevious(jobApplications.hasPrevious())
                .nextPage(jobApplications.hasNext() ? currentPage + 1 : null)
                .previousPage(currentPage > 0 ? currentPage - 1 : null)
                .build();
    }


//    private JobApplicationListDto getApplicationsSlice(Pageable pageable) {
//        Slice<JobApplication> jobApplications = jobApplicationRepository.findAll(pageable);
//
//        int currentPage = jobApplications.getNumber();
//
//        return JobApplicationListDto.builder()
//                .applications(jobApplications.getContent()
//                        .stream()
//                        .map(jobApplicationMapper::toCardDto)
//                        .toList())
//                .currentPage(currentPage)
//                .itemsPerPage(jobApplications.getSize())
//                .hasNext(jobApplications.hasNext())
//                .nextPage(jobApplications.hasNext() ? currentPage + 1 : null)
//                .previousPage(currentPage > 0 ? currentPage - 1 : null)
//                .build();
//    }

    @Override
    public JobApplicationListDto getApplicationsWithFilters(
            ApplicationStatus status,
            PerformerProfile profile,
            String country,
            String searchQuery,
            Pageable pageable) {

        log.debug("Получение заявок с фильтрами: status={}, profile={}, country={}, query={}, page={}, size={}",
                status, profile, country, searchQuery, pageable.getPageNumber(), pageable.getPageSize());

        Page<JobApplication> page = jobApplicationRepository.findWithFilters(
                status, profile, country, searchQuery, pageable);
        return buildListDto(page);
    }

    @Override
    public JobApplicationDto getApplicationDetails(Long id) {
        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена"));
        JobApplicationDto dto = jobApplicationMapper.toDto(jobApplication);
        return dto;
    }




    /**
     * Преобразует Page<JobApplication> в JobApplicationListDto
     */
    private JobApplicationListDto buildListDto(Page<JobApplication> page) {
        List<JobApplicationCardDto> cards = jobApplicationMapper.toCardDtoList(page.getContent());

        return JobApplicationListDto.builder()
                .applications(cards)
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .totalItems(page.getTotalElements())
                .itemsPerPage(page.getSize())
                .build();
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

    private StorageService.StorageResult uploadProfilePicture(MultipartFile file) {
        try {
            log.debug("Загрузка изображения профиля. Размер файла: {} bytes", file.getSize());

            imageValidator.validateImage(file);

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