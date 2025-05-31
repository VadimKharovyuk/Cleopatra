package com.example.cleopatra.service;

import com.example.cleopatra.dto.JobVacancy.CreateJobVacancyDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyListDto;
import org.springframework.data.domain.Pageable;

public interface JobVacancyService {
    JobVacancyDto createVacancy(CreateJobVacancyDto createJobVacancyDto);

    JobVacancyListDto listVacancies(Pageable pageable);

    JobVacancyDto getVacancy(Long id);
    JobVacancyDto  updateVacancy(JobVacancyDto jobVacancyDto);
    void deleteVacancy(Long id);

}
