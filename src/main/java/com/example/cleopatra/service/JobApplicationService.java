package com.example.cleopatra.service;
import com.example.cleopatra.dto.JobApplication.CreateJobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationListDto;
import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.enums.PerformerProfile;
import org.springframework.data.domain.Pageable;

public interface JobApplicationService {
    JobApplicationDto createJobApplication( CreateJobApplicationDto jobApplicationDto);

    JobApplicationListDto getAllApplications(Pageable pageable);

    JobApplicationListDto getApplicationsWithFilters(ApplicationStatus status, PerformerProfile profile, String country, String search, Pageable pageable);

    JobApplicationDto getApplicationDetails(Long id);



}
