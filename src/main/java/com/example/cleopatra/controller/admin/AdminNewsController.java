package com.example.cleopatra.controller.admin;

import com.example.cleopatra.dto.ProjectNews.CreateProjectNewsRequest;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsPageResponse;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsResponse;
import com.example.cleopatra.dto.ProjectNews.UpdateProjectNewsRequest;
import com.example.cleopatra.enums.NewsType;
import com.example.cleopatra.service.ProjectNewsService;
import com.example.cleopatra.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/news")
@RequiredArgsConstructor
@Slf4j
public class AdminNewsController {


    private final ProjectNewsService projectNewsService;
    private final UserService userService;

    /**
     * Главная страница управления новостями
     */
    @GetMapping
    public String adminNewsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NewsType newsType,
            @RequestParam(required = false) Boolean isPublished,
            Model model
    ) {
        log.info("Открытие админской страницы новостей: page={}, size={}, newsType={}, isPublished={}",
                page, size, newsType, isPublished);

        ProjectNewsPageResponse newsPage = projectNewsService.getAllNewsForAdmin(page, size, newsType, isPublished);

        model.addAttribute("newsPage", newsPage);
        model.addAttribute("newsTypes", NewsType.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedType", newsType);
        model.addAttribute("selectedPublished", isPublished);

        return "admin/news/list";
    }

    @GetMapping("/create")
    public String createNewsForm(Model model) {
        model.addAttribute("newsRequest", new CreateProjectNewsRequest());
        model.addAttribute("newsTypes", NewsType.values());
        return "admin/news/create";
    }

    @PostMapping("/create")
    public String createNews(
            @Valid @ModelAttribute("newsRequest") CreateProjectNewsRequest request,
            BindingResult bindingResult,
           Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("newsTypes", NewsType.values());
            return "admin/news/create";
        }

        try {
            Long authorId = getCurrentUserId(authentication);
            ProjectNewsResponse response = projectNewsService.createNews(request, authorId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Новость успешно создана: " + response.getTitle());

            return "redirect:/admin/news";
        } catch (Exception e) {
            log.error("Ошибка при создании новости: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Ошибка при создании новости: " + e.getMessage());
            model.addAttribute("newsTypes", NewsType.values());
            return "admin/news/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editNewsForm(@PathVariable Long id, Model model) {
        try {
            ProjectNewsResponse news = projectNewsService.getNewsById(id);

            UpdateProjectNewsRequest updateRequest = new UpdateProjectNewsRequest();
            updateRequest.setTitle(news.getTitle());
            updateRequest.setDescription(news.getDescription());
            updateRequest.setShortDescription(news.getShortDescription());
            updateRequest.setNewsType(news.getNewsType());

            model.addAttribute("news", news);
            model.addAttribute("updateRequest", updateRequest);
            model.addAttribute("newsTypes", NewsType.values());

            return "admin/news/edit";
        } catch (Exception e) {
            log.error("Ошибка при получении новости для редактирования: {}", e.getMessage(), e);
            return "redirect:/admin/news?error=Новость не найдена";
        }
    }

    @PostMapping("/{id}/update")
    public String updateNews(
            @PathVariable Long id,
            @Valid @ModelAttribute("updateRequest") UpdateProjectNewsRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            try {
                ProjectNewsResponse news = projectNewsService.getNewsById(id);
                model.addAttribute("news", news);
                model.addAttribute("newsTypes", NewsType.values());
                return "admin/news/edit";
            } catch (Exception e) {
                return "redirect:/admin/news?error=Новость не найдена";
            }
        }

        try {
            ProjectNewsResponse response = projectNewsService.updateNews(id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Новость успешно обновлена: " + response.getTitle());

            return "redirect:/admin/news";
        } catch (Exception e) {
            log.error("Ошибка при обновлении новости: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Ошибка при обновлении новости: " + e.getMessage());

            try {
                ProjectNewsResponse news = projectNewsService.getNewsById(id);
                model.addAttribute("news", news);
                model.addAttribute("newsTypes", NewsType.values());
                return "admin/news/edit";
            } catch (Exception ex) {
                return "redirect:/admin/news?error=Новость не найдена";
            }
        }
    }

    @PostMapping("/{id}/publish")
    public String publishNews(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            projectNewsService.publishNews(id);
            redirectAttributes.addFlashAttribute("successMessage", "Новость успешно опубликована");
        } catch (Exception e) {
            log.error("Ошибка при публикации новости: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при публикации: " + e.getMessage());
        }

        return "redirect:/admin/news";
    }


    @PostMapping("/{id}/unpublish")
    public String unpublishNews(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            projectNewsService.unpublishNews(id);
            redirectAttributes.addFlashAttribute("successMessage", "Новость снята с публикации");
        } catch (Exception e) {
            log.error("Ошибка при снятии новости с публикации: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при снятии с публикации: " + e.getMessage());
        }

        return "redirect:/admin/news";
    }


    @PostMapping("/{id}/delete")
    public String deleteNews(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            projectNewsService.deleteNews(id);
            redirectAttributes.addFlashAttribute("successMessage", "Новость успешно удалена");
        } catch (Exception e) {
            log.error("Ошибка при удалении новости: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении: " + e.getMessage());
        }

        return "redirect:/admin/news";
    }


    @GetMapping("/{id}")
    public String viewNews(@PathVariable Long id, Model model) {
        try {
            ProjectNewsResponse news = projectNewsService.getNewsById(id);
            model.addAttribute("news", news);
            return "admin/news/view";
        } catch (Exception e) {
            log.error("Ошибка при получении новости: {}", e.getMessage(), e);
            return "redirect:/admin/news?error=Новость не найдена";
        }
    }


    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}