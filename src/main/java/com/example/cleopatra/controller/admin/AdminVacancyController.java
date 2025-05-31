package com.example.cleopatra.controller.admin;


import com.example.cleopatra.dto.JobVacancy.CreateJobVacancyDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyDto;
import com.example.cleopatra.dto.JobVacancy.JobVacancyListDto;
import com.example.cleopatra.enums.PerformerProfile;
import com.example.cleopatra.service.JobVacancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/vacancies")
@RequiredArgsConstructor
@Slf4j
public class AdminVacancyController {

    private final JobVacancyService jobVacancyService;

    // Список вакансий для админа
    @GetMapping
    public String listVacancies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
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

        return "admin/vacancies/list";
    }

    // Просмотр вакансии
    @GetMapping("/view")
    public String viewVacancy(@RequestParam Long id, Model model) {
        log.debug("Просмотр вакансии с ID: {}", id);

        JobVacancyDto vacancy = jobVacancyService.getVacancy(id);
        model.addAttribute("vacancy", vacancy);
        model.addAttribute("vacancyId", id);

        return "admin/vacancies/view";
    }

    // Форма создания вакансии
    @GetMapping("/create")
    public String createVacancyForm(Model model) {
        model.addAttribute("vacancy", new CreateJobVacancyDto());
        model.addAttribute("requiredProfile", PerformerProfile.values());
        return "admin/vacancies/create";
    }

    // Создание вакансии
    @PostMapping("/create")
    public String createVacancy(@ModelAttribute CreateJobVacancyDto createJobVacancyDto,
                                RedirectAttributes redirectAttributes) {
        try {
            JobVacancyDto createdVacancy = jobVacancyService.createVacancy(createJobVacancyDto);
            redirectAttributes.addFlashAttribute("successMessage", "Вакансия успешно создана!");
            return "redirect:/admin/vacancies/view?id=" + createdVacancy.getId();
        } catch (Exception e) {
            log.error("Ошибка при создании вакансии", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при создании вакансии: " + e.getMessage());
            return "redirect:/admin/vacancies/create";
        }
    }

    // Форма редактирования
    @GetMapping("/edit")
    public String editVacancyForm(@RequestParam Long id, Model model) {
        JobVacancyDto vacancy = jobVacancyService.getVacancy(id);
        model.addAttribute("vacancy", vacancy);
        return "admin/vacancies/edit";
    }

    // Обновление вакансии
    @PostMapping("/edit")
    public String updateVacancy(@ModelAttribute JobVacancyDto jobVacancyDto,
                                RedirectAttributes redirectAttributes) {
        try {
            jobVacancyService.updateVacancy(jobVacancyDto);
            redirectAttributes.addFlashAttribute("successMessage", "Вакансия успешно обновлена!");
            return "redirect:/admin/vacancies/view?id=" + jobVacancyDto.getId();
        } catch (Exception e) {
            log.error("Ошибка при обновлении вакансии", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении: " + e.getMessage());
            return "redirect:/admin/vacancies/edit?id=" + jobVacancyDto.getId();
        }
    }

    // Удаление вакансии
    @PostMapping("/delete")
    public String deleteVacancy(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            jobVacancyService.deleteVacancy(id);
            redirectAttributes.addFlashAttribute("successMessage", "Вакансия успешно удалена!");
        } catch (Exception e) {
            log.error("Ошибка при удалении вакансии", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении: " + e.getMessage());
        }
        return "redirect:/admin/vacancies";
    }
}
