package com.example.cleopatra.controller;

import com.example.cleopatra.dto.ProjectNews.ProjectNewsPageResponse;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsResponse;
import com.example.cleopatra.enums.NewsType;
import com.example.cleopatra.service.ProjectNewsService;
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

    /**
     * Главная страница новостей проекта
     */
    @GetMapping
    public String newsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NewsType newsType,
            Model model
    ) {
        log.info("Открытие страницы новостей: page={}, size={}, newsType={}", page, size, newsType);

        ProjectNewsPageResponse newsPage = projectNewsService.getPublishedNews(page, size, newsType);

        model.addAttribute("newsPage", newsPage);
        model.addAttribute("newsTypes", NewsType.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedType", newsType);

        return "news/list";
    }

    /**
     * Просмотр конкретной новости
     */
    @GetMapping("/{id}")
    public String viewNews(@PathVariable Long id, Model model) {
        try {
            ProjectNewsResponse news = projectNewsService.getPublishedNewsById(id);

            // Увеличиваем счетчик просмотров
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