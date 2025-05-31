package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.JobVacancy.CreateJobVacancyDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyListDto;
import com.example.cleopatra.maper.JobVacancyMapper;
import com.example.cleopatra.repository.JobVacancyRepository;
import com.example.cleopatra.service.JobVacancyService;
import com.example.cleopatra.service.StorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import com.example.cleopatra.dto.JobVacancy.JobVacancyCardDto;
import com.example.cleopatra.maper.JobVacancyMapper;
import com.example.cleopatra.model.JobVacancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class JobVacancyServiceImpl implements JobVacancyService {

    private final JobVacancyRepository jobVacancyRepository;
    private final StorageService storageService;
    private final JobVacancyMapper jobVacancyMapper;

    @Override
    public JobVacancyDto createVacancy(CreateJobVacancyDto createJobVacancyDto) {
        try {
            JobVacancy jobVacancy = jobVacancyMapper.toEntity(createJobVacancyDto);

            if (createJobVacancyDto.getCompanyLogo() != null && !createJobVacancyDto.getCompanyLogo().isEmpty()) {
                StorageService.StorageResult storageResult = storageService.uploadImage(createJobVacancyDto.getCompanyLogo());
                jobVacancy.setCompanyLogoUrl(storageResult.getUrl());
                jobVacancy.setCompanyLogoId(storageResult.getImageId());
            }

            JobVacancy savedVacancy = jobVacancyRepository.save(jobVacancy);
            return jobVacancyMapper.toDto(savedVacancy);

        } catch (IOException e) {
            log.error("Ошибка при загрузке логотипа компании", e);
            throw new RuntimeException("Ошибка при создании вакансии", e);
        }
    }


    @Override
    public JobVacancyListDto listVacancies(Pageable pageable) {
        Slice<JobVacancy> vacancySlice = jobVacancyRepository.findAllBy(pageable);

        List<JobVacancyCardDto> vacancyCards = vacancySlice.getContent()
                .stream()
                .map(jobVacancyMapper::toCardDto)
                .toList();

        int currentPage = vacancySlice.getNumber();

        return JobVacancyListDto.builder()
                .vacancies(vacancyCards)
                .currentPage(currentPage)
                .itemsPerPage(vacancySlice.getSize())
                .totalPages(null) // Slice не знает общее количество страниц
                .totalItems(null) // Slice не знает общее количество элементов
                .hasNext(vacancySlice.hasNext())
                .hasPrevious(vacancySlice.hasPrevious())
                .nextPage(vacancySlice.hasNext() ? currentPage + 1 : null)
                .previousPage(currentPage > 0 ? currentPage - 1 : null)
                .build();
    }

    @Override
    public JobVacancyDto getVacancy(Long id) {
        JobVacancy jobVacancy = jobVacancyRepository.findById(id).orElse(null);
        return jobVacancyMapper.toDto(jobVacancy);
    }

    @Override
    public JobVacancyDto updateVacancy(JobVacancyDto jobVacancyDto) {
        if (jobVacancyDto.getId() == null) {
            throw new IllegalArgumentException("ID вакансии не может быть null");
        }
        JobVacancy existingVacancy = jobVacancyRepository.findById(jobVacancyDto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Вакансия с ID " + jobVacancyDto.getId() + " не найдена"));

        jobVacancyMapper.updateEntityFromDto(jobVacancyDto, existingVacancy);

        JobVacancy updatedVacancy = jobVacancyRepository.save(existingVacancy);
        return jobVacancyMapper.toDto(updatedVacancy);
    }

    @Override
    public void deleteVacancy(Long id) {
        JobVacancy existingVacancy = jobVacancyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия с ID " + id + " не найдена"));

        jobVacancyRepository.delete(existingVacancy);
    }
}