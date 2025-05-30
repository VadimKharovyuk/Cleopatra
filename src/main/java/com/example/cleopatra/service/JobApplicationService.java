package com.example.cleopatra.service;

import com.example.cleopatra.dto.JobApplication.CreateJobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationDto;

public interface JobApplicationService {
    JobApplicationDto createJobApplication( CreateJobApplicationDto jobApplicationDto);
}
