package com.example.cleopatra.controller;

import com.example.cleopatra.dto.VisitDto.VisitDtoCard;
import com.example.cleopatra.dto.VisitDto.VisitStatsDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.UserService;
import com.example.cleopatra.service.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/views")
@RequiredArgsConstructor
@Slf4j
public class ProfileViewController {

    private final VisitService visitService;
    private final UserService userService;


    @GetMapping()
    public String showProfileViews(@RequestParam(defaultValue = "30") int days,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Проверяем авторизацию
            if (authentication == null || !authentication.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Необходимо войти в систему");
                return "redirect:/auth/login";
            }

            // Получаем текущего пользователя
            UserResponse currentUser;
            try {
                currentUser = userService.getUserByEmail(authentication.getName());
                if (currentUser == null) {
                    log.warn("Пользователь не найден по email: {}", authentication.getName());
                    redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
                    return "redirect:/auth/login";
                }
            } catch (Exception e) {
                log.error("Ошибка при получении пользователя: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при получении данных пользователя");
                return "redirect:/error";
            }

            model.addAttribute("currentUser", currentUser);

            // Валидация параметра days
            if (days <= 0) {
                days = 30;
            }
            if (days > 365) {
                days = 365; // Максимум год
            }
            model.addAttribute("selectedDays", days);

            // Получаем период
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.minusDays(days);


            // Получаем уникальных посетителей за период
            List<VisitDtoCard> visitors;
            try {
                visitors = visitService.getUniqueVisitors(currentUser.getId(), from, to);
                if (visitors == null) {
                    visitors = new ArrayList<>();
                }
            } catch (Exception e) {
                log.error("Ошибка при получении посетителей: {}", e.getMessage());
                visitors = new ArrayList<>();
                model.addAttribute("errorMessage", "Не удалось загрузить список посетителей");
            }

            model.addAttribute("visitors", visitors);

            // Получаем статистику визитов
            VisitStatsDto visitStats;
            try {
                visitStats = visitService.getVisitStats(currentUser.getId());
                if (visitStats == null) {
                    visitStats = VisitStatsDto.builder()
                            .totalVisits(0L)
                            .todayVisits(0L)
                            .weekVisits(0L)
                            .monthVisits(0L)
                            .uniqueVisitorsCount(0L)
                            .build();
                }
            } catch (Exception e) {
                log.error("Ошибка при получении статистики: {}", e.getMessage());
                visitStats = VisitStatsDto.builder()
                        .totalVisits(0L)
                        .todayVisits(0L)
                        .weekVisits(0L)
                        .monthVisits(0L)
                        .uniqueVisitorsCount(0L)
                        .build();
                model.addAttribute("errorMessage", "Не удалось загрузить статистику");
            }

            model.addAttribute("visitStats", visitStats);

            // Добавляем варианты периодов для фильтра
            Map<Integer, String> periodOptions = new LinkedHashMap<>();
            periodOptions.put(1, "За сегодня");
            periodOptions.put(7, "За неделю");
            periodOptions.put(30, "За месяц");
            periodOptions.put(90, "За 3 месяца");
            periodOptions.put(365, "За год");
            model.addAttribute("periodOptions", periodOptions);

            log.debug("Успешно загружена страница просмотров для пользователя {}", currentUser.getId());
            return "profile/views";


        } catch (Exception e) {
            log.error("Критическая ошибка при показе просмотров профиля: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при загрузке страницы");
            return "redirect:/error";
        }
    }
}
