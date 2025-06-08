package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.Comment.*;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.service.CommentService;
import com.example.cleopatra.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CommentRestController {

    private final CommentService commentService;
    private final PostService postService;

    /**
     * Получить комментарии к посту (AJAX)
     * GET /api/posts/{postId}/comments
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentPageResponse> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("API: Получение комментариев для поста {}, страница {}", postId, page);

        try {
            Pageable pageable = PageRequest.of(page, size);
            CommentPageResponse response = commentService.getCommentsByPost(postId, pageable);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при получении комментариев для поста {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Создать новый комментарий (AJAX)
     * POST /api/posts/{postId}/comments
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("API: Создание комментария к посту {} от пользователя {}", postId,
                userDetails != null ? userDetails.getUsername() : "anonymous");

        try {
            if (userDetails == null) {
                log.warn("Попытка создания комментария неавторизованным пользователем");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Необходимо войти в систему"));
            }

            // Проверяем, что пост существует
            try {
                PostResponseDto post = postService.getPostById(postId);
                if (post == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Пост не найден"));
                }
            } catch (Exception e) {
                log.error("Ошибка при проверке существования поста {}: {}", postId, e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Пост не найден"));
            }

            CommentResponse response = commentService.createComment(postId, request, userDetails.getUsername());
            log.info("Комментарий успешно создан: {}", response.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Комментарий успешно добавлен",
                            "comment", response
                    ));
        } catch (IllegalArgumentException e) {
            log.warn("Некорректные данные при создании комментария: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка при создании комментария к посту {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при создании комментария: " + e.getMessage()));
        }
    }

    /**
     * Обновить комментарий (AJAX)
     * PUT /api/posts/{postId}/comments/{commentId}
     */
    @PutMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("API: Обновление комментария {} для поста {} от пользователя {}",
                commentId, postId, userDetails != null ? userDetails.getUsername() : "anonymous");

        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Необходимо войти в систему"));
            }

            CommentResponse response = commentService.updateComment(commentId, request, userDetails.getUsername());
            log.info("Комментарий {} успешно обновлен", commentId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Комментарий успешно обновлен",
                    "comment", response
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Некорректные данные при обновлении комментария {}: {}", commentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Попытка несанкционированного обновления комментария {}: {}", commentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Недостаточно прав для редактирования комментария"));
        } catch (Exception e) {
            log.error("Ошибка при обновлении комментария {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при обновлении комментария: " + e.getMessage()));
        }
    }

    /**
     * Удалить комментарий (AJAX)
     * DELETE /api/posts/{postId}/comments/{commentId}
     */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("API: Удаление комментария {} для поста {} от пользователя {}",
                commentId, postId, userDetails != null ? userDetails.getUsername() : "anonymous");

        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Необходимо войти в систему"));
            }

            CommentActionResponse response = commentService.deleteComment(commentId, userDetails.getUsername());
            log.info("Комментарий {} успешно удален", commentId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Комментарий успешно удален",
                    "result", response
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Комментарий {} не найден: {}", commentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Комментарий не найден"));
        } catch (SecurityException e) {
            log.warn("Попытка несанкционированного удаления комментария {}: {}", commentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Недостаточно прав для удаления комментария"));
        } catch (Exception e) {
            log.error("Ошибка при удалении комментария {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при удалении комментария: " + e.getMessage()));
        }
    }

    /**
     * Получить конкретный комментарий (AJAX)
     * GET /api/comments/{commentId}
     */
    @GetMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> getComment(@PathVariable Long commentId) {

        log.debug("API: Получение комментария {}", commentId);

        try {
            CommentResponse response = commentService.getCommentById(commentId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "comment", response
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Комментарий {} не найден: {}", commentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Комментарий не найден"));
        } catch (Exception e) {
            log.error("Ошибка при получении комментария {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении комментария"));
        }
    }

    /**
     * Получить количество комментариев к посту (AJAX)
     * GET /api/posts/{postId}/comments/count
     */
    @GetMapping("/posts/{postId}/comments/count")
    public ResponseEntity<Map<String, Object>> getCommentsCount(@PathVariable Long postId) {

        log.debug("API: Подсчет комментариев для поста {}", postId);

        try {
            long count = commentService.getCommentsCount(postId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));
        } catch (Exception e) {
            log.error("Ошибка при подсчете комментариев для поста {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", 0L
            ));
        }
    }

    /**
     * Получить последние комментарии к посту (AJAX)
     * GET /api/posts/{postId}/comments/latest
     */
    @GetMapping("/posts/{postId}/comments/latest")
    public ResponseEntity<Map<String, Object>> getLatestComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "3") int limit) {

        log.debug("API: Получение {} последних комментариев для поста {}", limit, postId);

        try {
            List<CommentResponse> comments = commentService.getLatestComments(postId, limit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "comments", comments
            ));
        } catch (Exception e) {
            log.error("Ошибка при получении последних комментариев для поста {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "comments", Collections.emptyList()
            ));
        }
    }

    /**
     * Получить комментарии пользователя (AJAX)
     * GET /api/users/{userId}/comments
     */
    @GetMapping("/users/{userId}/comments")
    public ResponseEntity<CommentPageResponse> getUserComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("API: Получение комментариев пользователя {}, страница {}", userId, page);

        try {
            Pageable pageable = PageRequest.of(page, size);
            CommentPageResponse response = commentService.getUserComments(userId, pageable);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при получении комментариев пользователя {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== EXCEPTION HANDLERS ====================

    /**
     * Обработчик исключений для REST контроллера
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Необработанная ошибка в CommentRestController: {}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Внутренняя ошибка сервера",
                        "message", e.getMessage()
                ));
    }

    /**
     * Обработчик ошибок валидации
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException e) {

        log.warn("Ошибка валидации: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Некорректные данные",
                        "message", "Проверьте правильность заполнения формы"
                ));
    }

    /**
     * Обработчик ошибок авторизации
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException e) {

        log.warn("Ошибка доступа: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Доступ запрещен",
                        "message", "У вас недостаточно прав для выполнения этого действия"
                ));
    }
}