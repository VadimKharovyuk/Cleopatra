package com.example.cleopatra.controller;

import com.example.cleopatra.dto.Comment.*;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.CommentService;
import com.example.cleopatra.service.PostService;
import com.example.cleopatra.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;



    @GetMapping("/posts/{postId}/comments")
    public String showPostComments(@PathVariable Long postId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Отображение страницы комментариев для поста {}", postId);

        try {
            // Получаем пост
            PostResponseDto post = postService.getPostById(postId);
            if (post == null) {
                log.warn("Пост с ID {} не найден", postId);
                model.addAttribute("error", "Пост не найден");
                return "error/comment-error";
            }

            // Получаем текущего пользователя (если авторизован)
            UserResponse currentUser = null;
            if (userDetails != null) {
                try {
                    currentUser = userService.getUserByEmail(userDetails.getUsername());
                } catch (Exception e) {
                    log.warn("Не удалось найти пользователя по email {}: {}", userDetails.getUsername(), e.getMessage());
                }
            }


            // Получаем комментарии с пагинацией
            Pageable pageable = PageRequest.of(page, size);
            CommentPageResponse comments = commentService.getCommentsByPost(postId, pageable);

            // Получаем общее количество комментариев
            long totalComments = commentService.getCommentsCount(postId);

            // Добавляем данные в модель
            model.addAttribute("post", post);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("postId", postId);
            model.addAttribute("comments", comments);
            model.addAttribute("totalComments", totalComments);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            return "comments/post-comments";

        } catch (Exception e) {
            log.error("Ошибка при загрузке комментариев для поста {}: {}", postId, e.getMessage(), e);
            model.addAttribute("error", "Не удалось загрузить комментарии: " + e.getMessage());
            return "error/comment-error";
        }
    }

    /**
     * Отображение комментариев пользователя
     * GET /users/{userId}/comments
     */
    @GetMapping("/users/{userId}/comments")
    public String showUserComments(@PathVariable Long userId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Отображение комментариев пользователя {}", userId);

        try {
            // Получаем текущего пользователя
            UserResponse currentUser = null;
            if (userDetails != null) {
                try {
                    currentUser = userService.getUserByEmail(userDetails.getUsername());
                } catch (Exception e) {
                    log.warn("Не удалось найти пользователя по email {}: {}", userDetails.getUsername(), e.getMessage());
                }
            }

            Pageable pageable = PageRequest.of(page, size);
            CommentPageResponse comments = commentService.getUserComments(userId, pageable);

            model.addAttribute("currentUser", currentUser);
            model.addAttribute("userId", userId);
            model.addAttribute("comments", comments);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            return "comments/user-comments";

        } catch (Exception e) {
            log.error("Ошибка при загрузке комментариев пользователя {}: {}", userId, e.getMessage());
            model.addAttribute("error", "Не удалось загрузить комментарии пользователя");
            return "error/comment-error";
        }
    }


    // ==================== EXCEPTION HANDLERS ====================

    /**
     * Обработчик исключений для контроллера
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Необработанная ошибка в CommentController: {}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Внутренняя ошибка сервера",
                        "message", e.getMessage()
                ));
    }
}