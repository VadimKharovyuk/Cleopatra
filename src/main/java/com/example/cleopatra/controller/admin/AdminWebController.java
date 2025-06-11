package com.example.cleopatra.controller.admin;

import com.example.cleopatra.model.SystemBlock;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.UserBlockingService;

import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminWebController {

    private final UserBlockingService userBlockingService;
    private final UserRepository userRepository;

    /**
     * Страница поиска пользователей
     */
    @GetMapping("/search")
    public String searchPage(Model model) {
        return "admin/blocked-users/user-search";
    }
    /**
     * Разблокировка пользователя
     */
    @PostMapping("/{userId}/unblock")
    public String unblockUser(@PathVariable Long userId,
                              @RequestParam String reason,
                              Authentication authentication, // Изменено
                              RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущего админа по email
            String adminEmail = authentication.getName();
            User currentAdmin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            userBlockingService.unblockUser(userId, reason, currentAdmin.getId());
            redirectAttributes.addFlashAttribute("success", "Пользователь успешно разблокирован");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка разблокировки: " + e.getMessage());
            log.error("Error unblocking user {}: {}", userId, e.getMessage(), e);
        }
        return "redirect:/admin/users/search";
    }


    /**
     * Поиск пользователя
     */
    @PostMapping("/search")
    public String searchUser(@RequestParam String query, Model model) {
        try {
            var userResponse = userBlockingService.findUserByEmailOrName(query);
            model.addAttribute("user", userResponse);
            model.addAttribute("query", query);
            return "admin/blocked-users/user-search";
        } catch (Exception e) {
            model.addAttribute("error", "Пользователь не найден: " + e.getMessage());
            model.addAttribute("query", query);
            return "admin/blocked-users/user-search";
        }
    }

    /**
     * Блокировка пользователя
     */
    @PostMapping("/{userId}/block")
    public String blockUser(@PathVariable Long userId,
                            @RequestParam String reason,
                            Authentication authentication, // Изменено
                            RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущего админа по email
            String adminEmail = authentication.getName();
            User currentAdmin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            userBlockingService.blockUser(userId, reason, currentAdmin.getId());
            redirectAttributes.addFlashAttribute("success", "Пользователь успешно заблокирован");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка блокировки: " + e.getMessage());
            log.error("Error blocking user {}: {}", userId, e.getMessage(), e);
        }
        return "redirect:/admin/users/search";
    }



    /**
     * Список заблокированных пользователей
     */
    @GetMapping("/blocked")
    public String blockedUsers(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SystemBlock> blockedUsers = userBlockingService.getActiveBlocks(pageable);

        model.addAttribute("blockedUsers", blockedUsers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blockedUsers.getTotalPages());

        return "admin/blocked-users/blocked-users";
    }

    /**
     * История блокировок пользователя
     */
    @GetMapping("/{userId}/history")
    public String userBlockHistory(@PathVariable Long userId, Model model) {
        try {
            var history = userBlockingService.getUserBlockHistory(userId);
            model.addAttribute("history", history);
            model.addAttribute("userId", userId);
            return "admin/blocked-users/user-block-history";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка получения истории: " + e.getMessage());
            return "redirect:/admin/users/search";
        }
    }
}