package com.example.cleopatra.controller;
import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.MentionService;
import com.example.cleopatra.service.PostReportService;
import com.example.cleopatra.service.PostService;
import com.example.cleopatra.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final PostReportService reportService ;

    @GetMapping("/{id}")
    public String showPost(@PathVariable Long id,
                           Authentication authentication,
                           Model model) {

        String username = authentication.getName();
        Long currentUserId = userService.getUserIdByEmail(username);

        try {
            PostResponseDto post = postService.getPostById(id);

            if (post == null) {
                log.warn("Пост с ID {} не найден!", id);
                return "error/404";
            }

            // Основные атрибуты
            model.addAttribute("post", post);
            model.addAttribute("currentUserId", currentUserId);

            // Атрибуты для системы жалоб
            boolean alreadyReported = reportService.hasUserReportedPost(id, currentUserId);
            model.addAttribute("alreadyReported", alreadyReported);

            // Проверяем, является ли пользователь автором поста
            boolean isOwner = post.getAuthor() != null &&
                    currentUserId.equals(post.getAuthor().getId());
            model.addAttribute("isPostOwner", isOwner);

            // Добавляем причины жалоб для JavaScript
            model.addAttribute("reportReasons",
                    java.util.Arrays.asList(com.example.cleopatra.enums.ReportReason.values()));

            model.addAttribute("isAuthenticated", true);

            log.debug("Показ поста {} пользователю {} ({})", id, currentUserId, username);
            return "posts/view";

        } catch (Exception e) {
            log.error("Ошибка при загрузке поста {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Ошибка при загрузке поста");
            return "error/500";
        }
    }


    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("postCreateDto", new PostCreateDto());
        return "posts/create";
    }

    @PostMapping("/create")
    public String createPost(@Valid @ModelAttribute PostCreateDto postCreateDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model,
                             HttpServletRequest request) {


        log.info("PostCreateDto: {}", postCreateDto);

        request.getParameterMap().forEach((key, values) -> {
            log.info("Параметр {}: {}", key, Arrays.toString(values));
        });

        // Проверка валидации
        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации: {}", bindingResult.getAllErrors());
            model.addAttribute("postCreateDto", postCreateDto);
            return "posts/create";
        }

        try {
            PostResponseDto createdPost = postService.createPost(postCreateDto);
            redirectAttributes.addFlashAttribute("successMessage", "Пост успешно создан!");
            return "redirect:/posts/" + createdPost.getId();

        } catch (Exception e) {
            log.error("Ошибка при создании поста: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Ошибка при создании поста: " + e.getMessage());
            model.addAttribute("postCreateDto", postCreateDto);
            return "posts/create";
        }
    }





    /**
     * Получить посты пользователя
     */
    @GetMapping("/users/{userId}/posts")
    public String getUserPosts(@PathVariable Long userId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {

        try {
            PostListDto userPosts = postService.getUserPosts(userId, page, size);

            // Получаем информацию о владельце постов для отображения
            UserResponse userInfo = userService.getUserById(userId);

            model.addAttribute("posts", userPosts);
            model.addAttribute("userInfo", userInfo);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            log.info("Отображение постов пользователя {}: найдено {} постов на странице {}",
                    userId, userPosts.getNumberOfElements(), page);

            return "posts/user-posts";

        } catch (Exception e) {
            log.error("Ошибка при получении постов пользователя {}: {}", userId, e.getMessage());
            model.addAttribute("errorMessage", "Не удалось загрузить посты пользователя: " + e.getMessage());
            return "posts/error";
        }
    }



    @GetMapping("/my-posts")
    public String getMyPosts(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {

        try {

            // Получаем информацию о текущем пользователе
            User currentUser = userService.getCurrentUserEntity();

            PostListDto myPosts = postService.getMyPosts(page, size);
            model.addAttribute("posts", myPosts);


            model.addAttribute("userInfo", currentUser);
            model.addAttribute("isOwnProfile", true);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            log.info("Отображение собственных постов: найдено {} постов на странице {}",
                    myPosts.getNumberOfElements(), page);

            return "posts/user-posts";

        } catch (Exception e) {
            log.error("Ошибка при получении собственных постов: {}", e.getMessage());
            model.addAttribute("errorMessage", "Не удалось загрузить ваши посты: " + e.getMessage());
            return "posts/error";
        }
    }



    /**
     * API endpoint для создания поста (для AJAX)
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<PostResponseDto> createPostApi(@Valid @ModelAttribute PostCreateDto postCreateDto,
                                                         BindingResult bindingResult,
                                                         HttpServletRequest request) {

        // ✅ ДОБАВЛЯЕМ детальные логи для API endpoint
        log.info("=== ПОЛУЧЕН API ЗАПРОС НА СОЗДАНИЕ ПОСТА ===");
        log.info("PostCreateDto: {}", postCreateDto);

        // Проверяем все параметры запроса
        log.info("=== ВСЕ ПАРАМЕТРЫ API ЗАПРОСА ===");
        request.getParameterMap().forEach((key, values) -> {
            log.info("API Параметр {}: {}", key, Arrays.toString(values));
        });

        // Проверка валидации
        if (bindingResult.hasErrors()) {
            log.warn("API Ошибки валидации: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().build();
        }

        try {
            PostResponseDto createdPost = postService.createPost(postCreateDto);

            if (createdPost.getLocation() != null) {
                log.info("API Response Location ID: {}", createdPost.getLocation().getId());
                log.info("API Response Location Coordinates: ({}, {})",
                        createdPost.getLocation().getLatitude(), createdPost.getLocation().getLongitude());
            }

            return ResponseEntity.ok(createdPost);

        } catch (Exception e) {
            log.error("Ошибка при создании поста через API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }


    /**
     * API endpoint для AJAX загрузки постов пользователя
     */
    @GetMapping("/api/users/{userId}/posts")
    @ResponseBody
    public ResponseEntity<PostListDto> getUserPostsApi(@PathVariable Long userId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        try {
            PostListDto userPosts = postService.getUserPosts(userId, page, size);
            return ResponseEntity.ok(userPosts);
        } catch (Exception e) {
            log.error("Ошибка API при получении постов пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

    }
}
