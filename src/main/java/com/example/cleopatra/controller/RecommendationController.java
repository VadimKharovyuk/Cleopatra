package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserRecommendationListDto;
import com.example.cleopatra.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping
    public String showAllRecommendations(@RequestParam(defaultValue = "0") int page,
                                         @ModelAttribute("currentUserId") Long currentUserId,
                                         Model model) {
        if (currentUserId == null) {
            return "redirect:/login";
        }

        try {
            UserRecommendationListDto recommendations = recommendationService.getAllRecommendations(currentUserId, page);
            model.addAttribute("recommendations", recommendations);
            return "recommendations/list";
        } catch (Exception e) {
            log.error("Ошибка при загрузке страницы рекомендаций: {}", e.getMessage());
            return "error/500";
        }
    }

    // AJAX endpoint для подгрузки следующих страниц
    @GetMapping("/load-more")
    @ResponseBody
    public ResponseEntity<UserRecommendationListDto> loadMoreRecommendations(
            @RequestParam int page,
            @ModelAttribute("currentUserId") Long currentUserId) {

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UserRecommendationListDto recommendations = recommendationService.getAllRecommendations(currentUserId, page);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Ошибка при загрузке дополнительных рекомендаций: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint для поиска с фильтрами
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<UserRecommendationListDto> searchRecommendations(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "all") String followers,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("currentUserId") Long currentUserId) {

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // TODO: Реализовать поиск с фильтрами в сервисе
            UserRecommendationListDto recommendations = recommendationService.searchRecommendations(
                    currentUserId, query, sort, followers, status, page);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Ошибка при поиске рекомендаций: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
