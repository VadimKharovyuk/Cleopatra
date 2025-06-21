package com.example.cleopatra.controller;

import com.example.cleopatra.dto.Forum.*;
import com.example.cleopatra.enums.ForumType;
import com.example.cleopatra.service.ForumReadService;
import com.example.cleopatra.service.ForumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;


@Slf4j
@Controller
@RequestMapping("/forums")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService; // write operations
    private final ForumReadService forumReadService; // read operations

    // Главная страница форума со списком тем
    @GetMapping
    public String getAllForums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) ForumType forumType,
            Model model) {

        try {
            // ✅ Используем ForumReadService для чтения
            ForumPageResponseDTO forums = forumReadService.getAllForums(page, size, sortBy, sortDirection, forumType);

            model.addAttribute("forums", forums);
            model.addAttribute("currentPage", page);
            model.addAttribute("currentType", forumType);
            model.addAttribute("forumTypes", ForumType.values());
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDirection", sortDirection);

            log.debug("Отображена страница форумов: page={}, type={}", page, forumType);
            return "forum/list";

        } catch (Exception e) {
            log.error("Ошибка при загрузке списка форумов", e);
            model.addAttribute("errorMessage", "Ошибка при загрузке списка тем");
            model.addAttribute("forumTypes", ForumType.values());
            return "forum/list";
        }
    }

    // Поиск по форуму
    @GetMapping("/search")
    public String searchForums(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        try {
            // ✅ Используем ForumReadService для поиска
            ForumPageResponseDTO searchResults = forumReadService.searchForums(query, page, size);

            model.addAttribute("forums", searchResults);
            model.addAttribute("searchQuery", query);
            model.addAttribute("currentPage", page);

            log.info("Выполнен поиск: '{}', найдено {} результатов", query, searchResults.getTotalElements());
            return "forum/search-results";

        } catch (IllegalArgumentException e) {
            log.warn("Некорректный поисковый запрос: {}", query);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("searchQuery", query);
            return "forum/search-results";
        } catch (Exception e) {
            log.error("Ошибка при поиске: {}", query, e);
            model.addAttribute("errorMessage", "Ошибка при выполнении поиска");
            model.addAttribute("searchQuery", query);
            return "forum/search-results";
        }
    }

    // Просмотр конкретной темы
    @GetMapping("/{id}")
    public String viewForum(@PathVariable Long id,
                            Authentication authentication,
                            Model model) {

        try {
            // ✅ Используем ForumService для viewForum (увеличивает счетчик)
            ForumDetailDTO forum = forumService.viewForum(id, authentication.getName());

            model.addAttribute("forum", forum);

            model.addAttribute("isOwner", forum.getAuthorName() != null &&
                    forum.getAuthorName().equals(authentication.getName()));

            model.addAttribute("forumTypes", ForumType.values());
            model.addAttribute("isAdmin", authentication.getAuthorities()
                    .contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

            log.debug("Пользователь {} просматривает тему {}", authentication.getName(), id);
            return "forum/detail";

        } catch (Exception e) {
            log.error("Ошибка при просмотре темы {}", id, e);
            model.addAttribute("errorMessage", "Тема не найдена или произошла ошибка");
            return "forum/detail";
        }
    }

    // Форма создания новой темы
    @GetMapping("/create")
    public String showCreateForm(Model model, Authentication authentication) {
        // Проверяем аутентификацию (хотя Spring Security должен это делать)
        if (authentication == null) {
            return "redirect:/login";
        }

        model.addAttribute("forumCreateDTO", new ForumCreateDTO());
        model.addAttribute("forumTypes", ForumType.values());

        log.debug("Пользователь {} открыл форму создания темы", authentication.getName());
        return "forum/create";
    }

    // Создание новой темы
    @PostMapping("/create")
    public String createForum(@Valid @ModelAttribute ForumCreateDTO forumCreateDTO,
                              BindingResult bindingResult,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              Model model) {

        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации при создании темы пользователем {}", authentication.getName());
            model.addAttribute("forumTypes", ForumType.values());
            return "forum/create";
        }

        try {
            ForumCreateResponseDTO response = forumService.createForum(forumCreateDTO, authentication.getName());

            redirectAttributes.addFlashAttribute("successMessage", response.getMessage());
            log.info("Пользователь {} создал тему: {}", authentication.getName(), response.getTitle());

            return "redirect:/forums/" + response.getId();

        } catch (Exception e) {
            log.error("Ошибка при создании темы пользователем {}", authentication.getName(), e);
            model.addAttribute("errorMessage", "Ошибка при создании темы: " + e.getMessage());
            model.addAttribute("forumTypes", ForumType.values());
            return "forum/create";
        }
    }

    // Удаление темы
    @PostMapping("/{id}/delete")
    public String deleteForum(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {

        try {
            boolean isAdmin = authentication.getAuthorities()
                    .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

            forumService.deleteForum(id, authentication.getName(), isAdmin);

            redirectAttributes.addFlashAttribute("successMessage", "Тема успешно удалена");
            log.info("Тема {} удалена пользователем {} (admin: {})", id, authentication.getName(), isAdmin);

        } catch (Exception e) {
            log.error("Ошибка при удалении темы {} пользователем {}", id, authentication.getName(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении: " + e.getMessage());
        }

        return "redirect:/forums";
    }

    // Дополнительный endpoint для проверки существования темы (AJAX)
    @GetMapping("/{id}/exists")
    @ResponseBody
    public boolean checkForumExists(@PathVariable Long id) {
        return forumReadService.existsById(id);
    }

    // Статистика по типам форумов (для админки или аналитики)
    @GetMapping("/stats")
    public String getForumStats(Model model, Authentication authentication) {
        // Проверяем права админа
        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!isAdmin) {
            return "redirect:/forums";
        }

        try {
            // Получаем статистику по всем типам
            model.addAttribute("totalForums", forumReadService.getForumCountByType(null));

            for (ForumType type : ForumType.values()) {
                long count = forumReadService.getForumCountByType(type);
                model.addAttribute(type.name().toLowerCase() + "Count", count);
            }

            return "forum/stats";

        } catch (Exception e) {
            log.error("Ошибка при получении статистики", e);
            model.addAttribute("errorMessage", "Ошибка при загрузке статистики");
            return "forum/stats";
        }
    }
}