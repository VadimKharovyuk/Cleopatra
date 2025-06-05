package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.Category;
import com.example.cleopatra.model.SupportRequest;
import com.example.cleopatra.service.SupportRequestService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportUserController {

    private final SupportRequestService supportRequestService;
    private final UserService userService;

    /**
     * Показать форму создания заявки
     */
    @GetMapping("/create/{userId}")
    public String showCreateForm(Model model, @PathVariable Long userId) {
        model.addAttribute("categories", Category.values());
        UserResponse user = userService.getUserById(userId);
        model.addAttribute("user", user);
        return "support/create";
    }

    /**
     * Создать новую заявку
     */
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

            return "redirect:/support/list/" + userId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании заявки: " + e.getMessage());
            return "redirect:/support/create/" + userId;
        }
    }

    /**
     * Показать список заявок конкретного пользователя
     */
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
     * Альтернативный endpoint для списка заявок пользователя
     */
    @GetMapping("/user/{userId}")
    public String getUserRequests(@PathVariable Long userId, Model model) {
        List<SupportRequest> requests = supportRequestService.getUserRequests(userId);
        UserResponse user = userService.getUserById(userId);

        model.addAttribute("requests", requests);
        model.addAttribute("user", user);

        return "support/user-requests";
    }
}