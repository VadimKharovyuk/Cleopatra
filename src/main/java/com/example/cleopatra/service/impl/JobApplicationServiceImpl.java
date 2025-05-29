package com.example.cleopatra.service.impl;

import com.example.cleopatra.maper.JobApplicationMapper;
import com.example.cleopatra.repository.JobApplicationRepository;
import com.example.cleopatra.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationServiceImpl implements JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationMapper jobApplicationMapper;
}
