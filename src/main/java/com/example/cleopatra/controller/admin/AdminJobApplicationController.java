package com.example.cleopatra.controller.admin;
import com.example.cleopatra.dto.JobApplication.JobApplicationListDto;
import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.enums.PerformerProfile;
import com.example.cleopatra.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/job-applications")
@RequiredArgsConstructor
@Slf4j
public class AdminJobApplicationController {

    private final JobApplicationService jobApplicationListService;

    /**
     * Главная страница со списком всех заявок
     */
    @GetMapping
    public String showJobApplicationsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) PerformerProfile profile,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String search,
            Model model) {

        log.debug("Запрос списка заявок: page={}, size={}, sortBy={}, sortDir={}",
                page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        JobApplicationListDto applicationsDto;
        if (status != null || profile != null || country != null || search != null) {
            applicationsDto = jobApplicationListService.getApplicationsWithFilters(
                    status, profile, country, search, pageable);
        } else {
            applicationsDto = jobApplicationListService.getAllApplications(pageable);
        }

        // Основные данные (пагинация теперь в DTO)
        model.addAttribute("applicationsDto", applicationsDto);

        // Параметры для формы (нужны для сохранения состояния фильтров/сортировки)
        model.addAttribute("currentSortBy", sortBy);
        model.addAttribute("currentSortDir", sortDir);

        // Для фильтров
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedProfile", profile);
        model.addAttribute("selectedCountry", country);
        model.addAttribute("searchQuery", search);

        // Для выпадающих списков
        model.addAttribute("allStatuses", ApplicationStatus.values());
        model.addAttribute("allProfiles", PerformerProfile.values());

        return "admin/job-applications/list";
    }

    /**
     * Просмотр конкретной заявки
     */
    @GetMapping("/view")
    public String viewJobApplication(
            @RequestParam Long id,
            Model model) {

        log.debug("Просмотр заявки с ID: {}", id);

        // TODO: Добавить метод для получения полной заявки по ID
        // JobApplicationDto application = jobApplicationService.getById(id);
        // model.addAttribute("application", application);

        model.addAttribute("applicationId", id);

        return "admin/job-applications/view";
    }

    /**
     * Страница редактирования заявки
     */
    @GetMapping("/edit")
    public String editJobApplication(
            @RequestParam Long id,
            Model model) {

        log.debug("Редактирование заявки с ID: {}", id);

        // TODO: Добавить метод для получения заявки для редактирования
        model.addAttribute("applicationId", id);

        return "admin/job-applications/edit";
    }
}