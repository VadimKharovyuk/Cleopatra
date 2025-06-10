package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserRecommendationDto;
import com.example.cleopatra.dto.user.UserRecommendationListDto;
import com.example.cleopatra.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;



    /**
     * Просмотр всех пользователей с пагинацией
     */
    @GetMapping()
    public String allRecommendationsPage(
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("currentUserId") Long currentUserId,
            Model model) {

        if (currentUserId == null) {
            return "redirect:/login";
        }

        try {
            UserRecommendationListDto recommendations =
                    recommendationService.getAllRecommendations(currentUserId, page);

            model.addAttribute("recommendations", recommendations);
            model.addAttribute("currentUserId", currentUserId);
            model.addAttribute("isMainPage", false);

            return "recommendations/list";

        } catch (Exception e) {
            log.error("Ошибка при загрузке всех рекомендаций для пользователя {}: {}",
                    currentUserId, e.getMessage(), e);

            model.addAttribute("recommendations", createEmptyRecommendations());
            model.addAttribute("currentUserId", currentUserId); // ← И ЭТУ ТОЖЕ

            return "recommendations/list";
        }
    }

    /**
     * AJAX поиск рекомендаций
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<UserRecommendationListDto> searchRecommendations(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("currentUserId") Long currentUserId) {

        if (currentUserId == null) {
            log.warn("Попытка поиска рекомендаций без авторизации");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            log.info("Поиск рекомендаций для пользователя: {}, запрос: '{}', страница: {}",
                    currentUserId, query, page);

            UserRecommendationListDto recommendations;

            // Если запрос пустой, показываем всех пользователей
            if (query == null || query.trim().isEmpty()) {
                recommendations = recommendationService.getAllRecommendations(currentUserId, page);
            } else {
                recommendations = recommendationService.searchRecommendations(currentUserId, query, page);
            }

            log.info("Найдено {} рекомендаций для пользователя {}",
                    recommendations.getUserRecommendations().size(), currentUserId);

            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            log.error("Ошибка при поиске рекомендаций для пользователя {}: {}",
                    currentUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Загрузить больше рекомендаций (для кнопки "Загрузить еще")
     */
    @GetMapping("/load-more")
    @ResponseBody
    public ResponseEntity<UserRecommendationListDto> loadMoreRecommendations(
            @RequestParam(defaultValue = "") String query,
            @RequestParam int page,
            @ModelAttribute("currentUserId") Long currentUserId) {

        log.info("Загрузка дополнительных рекомендаций: пользователь {}, запрос '{}', страница {}",
                currentUserId, query, page);

        // Используем тот же метод, что и для поиска
        return searchRecommendations(query, page, currentUserId);
    }

    /**
     * API для получения топ рекомендаций (для виджетов или мобильного приложения)
     */
    @GetMapping("/top")
    @ResponseBody
    public ResponseEntity<List<UserRecommendationDto>> getTopRecommendations(
            @ModelAttribute("currentUserId") Long currentUserId) {

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<UserRecommendationDto> topRecommendations =
                    recommendationService.getTopRecommendations(currentUserId);
            return ResponseEntity.ok(topRecommendations);

        } catch (Exception e) {
            log.error("Ошибка при получении топ рекомендаций для пользователя {}: {}",
                    currentUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Создать пустой результат для обработки ошибок
     */
    private UserRecommendationListDto createEmptyRecommendations() {
        return UserRecommendationListDto.builder()
                .userRecommendations(Collections.emptyList())
                .currentPage(0)
                .itemsPerPage(12)
                .hasNext(false)
                .hasPrevious(false)
                .nextPage(null)
                .previousPage(null)
                .build();
    }
}
