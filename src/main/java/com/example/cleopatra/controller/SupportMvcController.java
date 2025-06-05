package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.Category;
import com.example.cleopatra.enums.Status;
import com.example.cleopatra.model.SupportRequest;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.SupportRequestService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportMvcController {

    private final SupportRequestService supportRequestService;
    private final UserService userService;

    /**
     * Показать форму создания заявки
     */
    @GetMapping("/create/{userId}")
    public String showCreateForm(Model model,
                                 @PathVariable Long userId) {
        model.addAttribute("categories", Category.values());

        UserResponse user = userService.getUserById(userId);
        model.addAttribute("user", user);

        return "support/create";
    }
    @PostMapping("/create")
    public String createSupportRequest(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Long userId,
            @RequestParam String category,
            RedirectAttributes redirectAttributes) {

        try {
            SupportRequest created = supportRequestService.createSupportRequest(
                    title, description, userId, category);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Заявка #" + created.getId() + " успешно создана!");

            // Редирект на список заявок пользователя
            return "redirect:/support/list/" + userId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании заявки: " + e.getMessage());

            return "redirect:/support/create/" + userId;
        }
    }
    @GetMapping("/list/{userId}")
    public String listUserSupportRequests(@PathVariable Long userId, Model model) {
        List<SupportRequest> requests = supportRequestService.getUserRequests(userId);
        UserResponse user = userService.getUserById(userId);

        model.addAttribute("requests", requests);
        model.addAttribute("user", user);

        return "support/list";
    }
    /**
     * Показать конкретную заявку
     */
    @GetMapping("/view/{id}")
    public String viewSupportRequest(@PathVariable Long id, Model model) {
        return supportRequestService.getSupportRequestById(id)
                .map(request -> {
                    model.addAttribute("request", request);
                    return "support/view";
                })
                .orElse("redirect:/support/list");
    }

    /**
     * Показать список всех заявок пользователя
     */
    @GetMapping("/user/{userId}")
    public String getUserRequests(@PathVariable Long userId, Model model) {
        List<SupportRequest> requests = supportRequestService.getUserRequests(userId);

        // Получаем информацию о пользователе
        UserResponse user = userService.getUserById(userId);

        model.addAttribute("requests", requests);
        model.addAttribute("user", user);

        return "support/user-requests";
    }

    /**
     * Админ панель - список всех активных заявок
     */
    @GetMapping("/admin")
    public String adminPanel(Model model) {
        List<SupportRequest> activeRequests = supportRequestService.getActiveRequests();
        SupportRequestService.SupportStatistics stats = supportRequestService.getStatistics();

        model.addAttribute("activeRequests", activeRequests);
        model.addAttribute("stats", stats);
        model.addAttribute("statuses", Status.values());

        return "support/admin";
    }

    /**
     * Обновить статус заявки (для админов)
     */
    @PostMapping("/admin/update-status")
    public String updateStatus(
            @RequestParam Long requestId,
            @RequestParam String status,
            @RequestParam(required = false) String adminResponse,
            RedirectAttributes redirectAttributes) {

        try {
            Status newStatus = Status.valueOf(status);

            supportRequestService.updateStatus(requestId, newStatus, adminResponse)
                    .ifPresentOrElse(
                            updated -> redirectAttributes.addFlashAttribute("successMessage",
                                    "Статус заявки #" + requestId + " обновлен!"),
                            () -> redirectAttributes.addFlashAttribute("errorMessage",
                                    "Заявка не найдена!")
                    );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при обновлении: " + e.getMessage());
        }

        return "redirect:/support/admin";
    }


}