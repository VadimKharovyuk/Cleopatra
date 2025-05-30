package com.example.cleopatra.controller;
import com.example.cleopatra.ExistsException.ImageUploadException;
import com.example.cleopatra.ExistsException.ImageValidationException;
import com.example.cleopatra.ExistsException.JobApplicationSaveException;
import com.example.cleopatra.dto.JobApplication.CreateJobApplicationDto;
import com.example.cleopatra.dto.JobApplication.JobApplicationDto;
import com.example.cleopatra.enums.PerformerProfile;
import com.example.cleopatra.service.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/job")
@RequiredArgsConstructor
@Slf4j
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    /**
     * Отображает форму создания заявки
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        log.debug("Отображение формы создания заявки");

        model.addAttribute("jobApplication", new CreateJobApplicationDto());
        model.addAttribute("performerProfiles", PerformerProfile.values());

        return "job/create-form";
    }

    /**
     * Обрабатывает отправку формы заявки
     */
    @PostMapping("/send")
    public String submitJobApplication(
            @Valid @ModelAttribute("jobApplication") CreateJobApplicationDto createJobApplicationDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Получена заявка от пользователя: {}", createJobApplicationDto.getEmail());

        // Проверяем наличие ошибок валидации
        if (bindingResult.hasErrors()) {
            log.warn("Форма содержит ошибки валидации для пользователя: {}",
                    createJobApplicationDto.getEmail());
            model.addAttribute("performerProfiles", PerformerProfile.values());
            return "job/create-form";
        }
        JobApplicationDto savedApplication = jobApplicationService.createJobApplication(createJobApplicationDto);

        log.info("Заявка успешно создана с ID: {} для пользователя: {}",
                savedApplication.getName(), createJobApplicationDto.getEmail());

        redirectAttributes.addFlashAttribute("successMessage",
                "Ваша заявка успешно отправлена! Мы свяжемся с вами в ближайшее время.");
        redirectAttributes.addFlashAttribute("applicationId", savedApplication.getName());

        return "redirect:/job/success";
    }

    /**
     * Страница успешной отправки заявки
     */
    @GetMapping("/success")
    public String showSuccessPage() {
        log.debug("Отображение страницы успешной отправки заявки");
        return "job/success";
    }

    /**
     * Список заявок (для администраторов)
     */
    @GetMapping
    public String showJobApplicationsList(Model model) {
        log.debug("Отображение списка заявок");
        // Здесь можно добавить логику получения списка заявок
        return "job/list";
    }
}
