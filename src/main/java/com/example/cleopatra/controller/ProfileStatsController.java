package com.example.cleopatra.controller;
import com.example.cleopatra.dto.user.ProfileStatisticsDTO;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.ProfileStatisticsService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/showProfileStats")
public class ProfileStatsController {

    private final UserService userService;
    private final ProfileStatisticsService profileStatisticsService;

    @GetMapping("/{userId}/stats")
    public String showProfileStats(@PathVariable Long userId,
                                   @ModelAttribute("currentUserId") Long currentUserId,
                                   Model model) {
        try {
            if (currentUserId == null) {
                return "redirect:/login?returnUrl=/profile/" + userId + "/stats";
            }

            // Проверка доступа (только владелец может видеть свою статистику)
            if (!currentUserId.equals(userId)) {
                return "redirect:/profile/" + userId + "?error=access_denied";
            }

            UserResponse user = userService.getUserById(userId);
            if (user == null) {
                return "error/404";
            }

            // Получаем реальную статистику
            ProfileStatisticsDTO stats = profileStatisticsService.getUserProfileStats(userId);

            model.addAttribute("user", user);
            model.addAttribute("stats", stats);
            model.addAttribute("currentUserId", currentUserId);

            return "profile/stats";

        } catch (Exception e) {
            log.error("Ошибка при загрузке статистики профиля {}: {}", userId, e.getMessage(), e);
            return "error/500";
        }
    }

    @GetMapping("/{userId}/stats/export")
    public String exportStats(@PathVariable Long userId,
                              @ModelAttribute("currentUserId") Long currentUserId) {
        // TODO: реализовать экспорт статистики в CSV/JSON
        log.info("Экспорт статистики для пользователя {}", userId);
        return "redirect:/profile/" + userId + "/stats?exported=true";
    }
}