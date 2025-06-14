package com.example.cleopatra.controller.admin;

import com.example.cleopatra.enums.Status;
import com.example.cleopatra.model.SupportRequest;
import com.example.cleopatra.service.SupportRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/support/admin")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')") // Ограничиваем доступ только для админов
public class SupportAdminController {

    private final SupportRequestService supportRequestService;

    /**
     * Админ панель - главная страница с активными заявками и статистикой
     */
    @GetMapping
    public String adminPanel(Model model) {
        List<SupportRequest> activeRequests = supportRequestService.getActiveRequests();
        SupportRequestService.SupportStatistics stats = supportRequestService.getStatistics();

        model.addAttribute("activeRequests", activeRequests);
        model.addAttribute("stats", stats);
        model.addAttribute("statuses", Status.values());

        return "admin/support/dashboard";
    }


    /**
     * Показать все заявки с определенным статусом
     */
    @GetMapping("/status/{status}")
    public String getRequestsByStatus(@PathVariable String status, Model model) {
        try {
            Status requestStatus = Status.valueOf(status.toUpperCase());
            List<SupportRequest> requests = supportRequestService.getRequestsByStatus(requestStatus);

            model.addAttribute("requests", requests);
            model.addAttribute("currentStatus", requestStatus);
            model.addAttribute("statuses", Status.values());

            return "admin/support/requests-by-status";

        } catch (IllegalArgumentException e) {
            return "redirect:/support/admin";
        }
    }

    /**
     * Показать детали конкретной заявки для админа
     */
    @GetMapping("/request/{id}")
    public String viewRequestDetails(@PathVariable Long id, Model model) {
        return supportRequestService.getSupportRequestById(id)
                .map(request -> {
                    model.addAttribute("request", request);
                    model.addAttribute("statuses", Status.values());
                    return "admin/support/request-details";
                })
                .orElse("redirect:/support/admin");
    }

    /**
     * Обновить статус заявки
     */
    @PostMapping("/update-status")
    public String updateStatus(
            @RequestParam Long requestId,
            @RequestParam String status,
            @RequestParam(required = false) String adminResponse,
            RedirectAttributes redirectAttributes) {

        try {
            Status newStatus = Status.valueOf(status.toUpperCase());

            supportRequestService.updateStatus(requestId, newStatus, adminResponse)
                    .ifPresentOrElse(
                            updated -> redirectAttributes.addFlashAttribute("successMessage",
                                    "Статус заявки #" + requestId + " обновлен на " + newStatus + "!"),
                            () -> redirectAttributes.addFlashAttribute("errorMessage",
                                    "Заявка не найдена!")
                    );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Некорректный статус: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при обновлении: " + e.getMessage());
        }

        return "redirect:/support/admin";
    }

    /**
     * Быстро взять заявку в работу
     */
    @PostMapping("/take/{requestId}")
    public String takeRequestInProgress(@PathVariable Long requestId,
                                        RedirectAttributes redirectAttributes) {
        supportRequestService.takeInProgress(requestId)
                .ifPresentOrElse(
                        updated -> redirectAttributes.addFlashAttribute("successMessage",
                                "Заявка #" + requestId + " взята в работу!"),
                        () -> redirectAttributes.addFlashAttribute("errorMessage",
                                "Заявка не найдена!")
                );

        return "redirect:/support/admin";
    }

    /**
     * Быстро закрыть заявку
     */
    @PostMapping("/close/{requestId}")
    public String closeRequest(@PathVariable Long requestId,
                               @RequestParam(required = false) String adminResponse,
                               RedirectAttributes redirectAttributes) {

        supportRequestService.closeRequest(requestId, adminResponse)
                .ifPresentOrElse(
                        updated -> redirectAttributes.addFlashAttribute("successMessage",
                                "Заявка #" + requestId + " закрыта!"),
                        () -> redirectAttributes.addFlashAttribute("errorMessage",
                                "Заявка не найдена!")
                );

        return "redirect:/support/admin";
    }

    /**
     * Показать статистику по заявкам
     */
    @GetMapping("/statistics")
    public String showStatistics(Model model) {
        SupportRequestService.SupportStatistics stats = supportRequestService.getStatistics();
        model.addAttribute("stats", stats);
        return "admin/support/statistics";
    }
}