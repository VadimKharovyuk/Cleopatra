package com.example.cleopatra.service;

import com.example.cleopatra.dto.JobApplication.CreateJobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationListDto;
import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.enums.PerformerProfile;
import org.springframework.data.domain.Pageable;

public interface JobApplicationService {
    JobApplicationDto createJobApplication( CreateJobApplicationDto jobApplicationDto);

    /**
     * Получить список всех заявок с пагинацией
     */
    JobApplicationListDto getAllApplications(Pageable pageable);

//    /**
//     * Получить список заявок с фильтрацией по статусу
//     */
//    JobApplicationListDto getApplicationsByStatus(ApplicationStatus status, Pageable pageable);
//
//    /**
//     * Получить список заявок с фильтрацией по профилю
//     */
//    JobApplicationListDto getApplicationsByProfile(PerformerProfile profile, Pageable pageable);
//
//    /**
//     * Получить список заявок с фильтрацией по стране
//     */
//    JobApplicationListDto getApplicationsByCountry(String country, Pageable pageable);
//
//    /**
//     * Поиск заявок по имени или email
//     */
//    JobApplicationListDto searchApplications(String searchQuery, Pageable pageable);
//
//    /**
//     * Получить список заявок с комплексной фильтрацией
//     */
//    JobApplicationListDto getApplicationsWithFilters(
//            ApplicationStatus status,
//            PerformerProfile profile,
//            String country,
//            String searchQuery,
//            Pageable pageable
//    );
}
