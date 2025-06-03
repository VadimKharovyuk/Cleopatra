package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.ExistsException.PostNotFoundException;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.PostService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostDeleteController {

    private final PostService postService;
    private final UserService userService;

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, Authentication authentication) {
        try {
            // Проверяем авторизацию
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Необходима авторизация"));
            }

            User currentUser = userService.getCurrentUserEntity();

            // Проверяем существование поста
            Post post = postService.findById(postId);
            if (post == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Пост не найден"));
            }

            // Проверяем права на удаление (только автор может удалить)
            if (!post.getAuthor().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Нет прав на удаление этого поста"));
            }

            // Удаляем пост
            postService.deletePost(postId);

            log.info("Пост {} удален пользователем {}", postId, currentUser.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Пост успешно удален"
            ));

        } catch (PostNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Пост не найден"));
        } catch (Exception e) {
            log.error("Ошибка при удалении поста {}: {}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Произошла ошибка при удалении поста"));
        }
    }
}





