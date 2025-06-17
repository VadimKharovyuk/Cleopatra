package com.example.cleopatra.controller;

import com.example.cleopatra.dto.ProjectNews.ProjectNewsPageResponse;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsResponse;
import com.example.cleopatra.enums.NewsType;
import com.example.cleopatra.service.ProjectNewsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final ProjectNewsService projectNewsService;


    @GetMapping
    public String newsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NewsType newsType,
            Model model
    ) {
        try {
            log.info("Getting news page: page={}, size={}, newsType={}", page, size, newsType);

            ProjectNewsPageResponse newsPage = projectNewsService.getPublishedNews(page, size, newsType);
            log.info("Successfully retrieved {} news items", newsPage.getContent().size());

            model.addAttribute("newsPage", newsPage);
            model.addAttribute("newsTypes", NewsType.values());
            model.addAttribute("currentPage", page);
            model.addAttribute("selectedType", newsType);

            return "news/list";
        } catch (Exception e) {
            log.error("Error getting news page", e);
            model.addAttribute("error", "Ошибка загрузки новостей: " + e.getMessage());
            return "error";
        }
    }

    @ExceptionHandler(Exception.class)
    public String handleError(Exception e, Model model) {
        log.error("Unexpected error", e);
        model.addAttribute("error", "Произошла ошибка: " + e.getMessage());
        return "error";
    }
    @PostConstruct
    public void init() {
        log.info("NewsController initialized with service: {}", projectNewsService);
    }


    @GetMapping("/{id}")
    public String viewNews(@PathVariable Long id, Model model) {
        try {
            ProjectNewsResponse news = projectNewsService.getPublishedNewsById(id);

            projectNewsService.incrementViewCount(id);

            model.addAttribute("news", news);
            return "news/view";
        } catch (Exception e) {
            log.error("Ошибка при получении новости: {}", e.getMessage(), e);
            return "redirect:/news?error=Новость не найдена";
        }
    }

    /**
     * API для получения новостей (для AJAX запросов)
     */
    @GetMapping("/api")
    @ResponseBody
    public ProjectNewsPageResponse getNewsApi(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NewsType newsType
    ) {
        log.debug("API запрос новостей: page={}, size={}, newsType={}", page, size, newsType);
        return projectNewsService.getPublishedNews(page, size, newsType);
    }
}