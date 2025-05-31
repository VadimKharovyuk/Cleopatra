package com.example.cleopatra.controller;

import com.example.cleopatra.dto.JobVacancy.JobVacancyDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyListDto;
import com.example.cleopatra.service.JobVacancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vacancies")
@RequiredArgsConstructor
@Slf4j
public class VacancyController {

    private final JobVacancyService jobVacancyService;

    // Список вакансий для пользователей
    @GetMapping
    public String listVacancies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        JobVacancyListDto vacanciesDto = jobVacancyService.listVacancies(pageable);

        model.addAttribute("vacanciesDto", vacanciesDto);
        model.addAttribute("currentSortBy", sortBy);
        model.addAttribute("currentSortDir", sortDir);

        return "vacancies/list";
    }


    @GetMapping("/{id}")
    public String viewVacancy(@PathVariable Long id, Model model) {
        log.debug("Просмотр вакансии с ID: {}", id);

        JobVacancyDto vacancy = jobVacancyService.getVacancy(id);
        model.addAttribute("vacancy", vacancy);
        model.addAttribute("vacancyId", id);

        return "vacancies/view";
    }

}