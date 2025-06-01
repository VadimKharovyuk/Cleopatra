package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/showProfileStats")
public class ProfileStatsController {
    private final UserService userService;


    @GetMapping("/{userId}/stats")
    public String showProfileStats(@PathVariable Long userId,
                                   @ModelAttribute("currentUserId") Long currentUserId,
                                   Model model) {
        try {
            // Проверка авторизации
            if (currentUserId == null) {
                return "redirect:/login?returnUrl=/profile/" + userId + "/stats";
            }

            // Проверка доступа (только владелец может видеть свою статистику)
            if (!currentUserId.equals(userId)) {
                return "redirect:/profile/" + userId + "?error=access_denied";
            }

            // Получаем данные пользователя
            UserResponse user = userService.getUserById(userId);
            if (user == null) {
                return "error/404";
            }

            model.addAttribute("user", user);

            // TODO: Добавить когда будет готов StatisticsService
            // ProfileStatistics stats = statisticsService.getUserStats(userId);
            // model.addAttribute("stats", stats);

            // Временные данные для демонстрации (потом удалить)
            addMockStatsData(model);

            return "profile/stats";

        } catch (Exception e) {
            log.error("Ошибка при загрузке статистики профиля {}: {}", userId, e.getMessage());
            return "error/500";
        }
    }

    // Временный метод для демонстрации (удалить когда будет готов StatisticsService)
    private void addMockStatsData(Model model) {
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("profileViews", 1247);
        mockStats.put("totalLikes", 3456);
        mockStats.put("avgEngagement", "7.2%");
        mockStats.put("postsCount", 89);
        mockStats.put("followersCount", 2534);
        mockStats.put("newFollowersWeek", 47);
        mockStats.put("unfollowersWeek", 12);
        mockStats.put("netGrowth", 35);

        model.addAttribute("stats", mockStats);
    }
}
