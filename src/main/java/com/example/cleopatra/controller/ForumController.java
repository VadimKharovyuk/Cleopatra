package com.example.cleopatra.controller;

import com.example.cleopatra.dto.Forum.*;
import com.example.cleopatra.dto.ForumComment.*;
import com.example.cleopatra.enums.ForumType;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.ForumCommentService;
import com.example.cleopatra.service.ForumReadService;
import com.example.cleopatra.service.ForumService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/forums")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService; // write operations
    private final ForumReadService forumReadService; // read operations
    private final ForumCommentService forumCommentService; // comment operations
    private final UserService userService;

    // ========== FORUM METHODS ==========

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
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Authentication authentication,
                            Model model) {

        try {
            // ✅ Используем ForumService для viewForum (увеличивает счетчик)
            ForumDetailDTO forum = forumService.viewForum(id, authentication.getName());

            // Получаем комментарии для форума
            ForumCommentPageDto comments = forumCommentService.getForumComments(id, page, size);

            model.addAttribute("forum", forum);
            model.addAttribute("comments", comments);
            model.addAttribute("currentPage", page);
            model.addAttribute("createCommentDto", new CreateForumCommentDto());

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

    // ========== COMMENT METHODS ==========

    // Создание комментария
    @PostMapping("/{forumId}/comments")
    public String createComment(@PathVariable Long forumId,
                                @Valid @ModelAttribute CreateForumCommentDto createCommentDto,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка валидации комментария");
            return "redirect:/forums/" + forumId;
        }

        try {
            // Устанавливаем ID форума в DTO
            createCommentDto.setForumId(forumId);

            // Получаем ID пользователя
            Long userId = userService.getUserIdByEmail(authentication.getName());

            ForumCommentDto comment = forumCommentService.createForumComment(createCommentDto, userId);

            redirectAttributes.addFlashAttribute("successMessage", "Комментарий успешно добавлен");
            log.info("Пользователь {} добавил комментарий к теме {}", authentication.getName(), forumId);

        } catch (Exception e) {
            log.error("Ошибка при создании комментария пользователем {} к теме {}",
                    authentication.getName(), forumId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при добавлении комментария: " + e.getMessage());
        }

        return "redirect:/forums/" + forumId;
    }


    @PostMapping("/{forumId}/comments/{parentId}/reply")
    public String replyToComment(@PathVariable Long forumId,
                                 @PathVariable Long parentId,
                                 @RequestParam String content,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        log.info("=== REPLY COMMENT DEBUG ===");
        log.info("ForumId: {}", forumId);
        log.info("ParentId: {}", parentId);
        log.info("Content: '{}'", content);
        log.info("User: {}", authentication.getName());

        try {
            // Валидация контента
            if (content == null || content.trim().length() < 3) {
                log.warn("Content too short: '{}'", content);
                redirectAttributes.addFlashAttribute("errorMessage", "Ответ должен содержать не менее 3 символов");
                return "redirect:/forums/" + forumId;
            }

            if (content.length() > 1000) {
                log.warn("Content too long: {} characters", content.length());
                redirectAttributes.addFlashAttribute("errorMessage", "Ответ не должен превышать 1000 символов");
                return "redirect:/forums/" + forumId;
            }

            CreateForumCommentDto replyDto = CreateForumCommentDto.builder()
                    .forumId(forumId)
                    .parentId(parentId)
                    .content(content.trim())
                    .build();

            log.info("Reply DTO created: {}", replyDto);

            // Получаем ID пользователя через ваш метод
            String userEmail = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(userEmail);

            if (currentUserId == null) {
                log.error("User not found by email: {}", userEmail);
                redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
                return "redirect:/forums/" + forumId;
            }

            ForumCommentDto reply = forumCommentService.createForumComment(replyDto, currentUserId);


            redirectAttributes.addFlashAttribute("successMessage", "Ответ успешно добавлен");

        } catch (Exception e) {
            log.error("Error creating reply", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при добавлении ответа: " + e.getMessage());
        }

        return "redirect:/forums/" + forumId + "#comment-" + parentId;
    }

    // Удаление комментария
    @PostMapping("/{forumId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long forumId,
                                @PathVariable Long commentId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        try {
            // Получаем ID пользователя
            Long userId = userService.getUserIdByEmail(authentication.getName());

            boolean deleted = forumCommentService.deleteForumComment(commentId, userId);

            if (deleted) {
                redirectAttributes.addFlashAttribute("successMessage", "Комментарий успешно удален");
                log.info("Пользователь {} удалил комментарий {} в теме {}",
                        authentication.getName(), commentId, forumId);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Не удалось удалить комментарий");
            }

        } catch (Exception e) {
            log.error("Ошибка при удалении комментария {} пользователем {} в теме {}",
                    commentId, authentication.getName(), forumId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении комментария: " + e.getMessage());
        }

        return "redirect:/forums/" + forumId;
    }

    // AJAX методы для комментариев

    // Получение ответов на комментарий (AJAX)
    @GetMapping("/{forumId}/comments/{commentId}/replies")
    @ResponseBody
    public ResponseEntity<List<ForumCommentDto>> getCommentReplies(@PathVariable Long forumId,
                                                                   @PathVariable Long commentId) {

        try {
            List<ForumCommentDto> replies = forumCommentService.getCommentReplies(commentId);
            return ResponseEntity.ok(replies);

        } catch (Exception e) {
            log.error("Ошибка при получении ответов на комментарий {}", commentId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Загрузка дополнительных комментариев (AJAX)
    @GetMapping("/{forumId}/comments")
    @ResponseBody
    public ResponseEntity<ForumCommentPageDto> getMoreComments(@PathVariable Long forumId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {

        try {
            ForumCommentPageDto comments = forumCommentService.getForumComments(forumId, page, size);
            return ResponseEntity.ok(comments);

        } catch (Exception e) {
            log.error("Ошибка при получении комментариев для форума {}", forumId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== UTILITY METHODS ==========

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