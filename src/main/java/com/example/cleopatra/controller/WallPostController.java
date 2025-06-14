// WallPostController.java
package com.example.cleopatra.controller;

import com.example.cleopatra.dto.WallPost.WallPostCardResponse;
import com.example.cleopatra.dto.WallPost.WallPostCreateRequest;
import com.example.cleopatra.dto.WallPost.WallPostPageResponse;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.SubscriptionService;
import com.example.cleopatra.service.UserService;
import com.example.cleopatra.service.WallPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/wall")
public class WallPostController {

    private final WallPostService wallPostService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;


//    @PostMapping("/wall/api/posts/{postId}/like")
//    @GetMapping("/wall/api/posts/{postId}/comments")


    /**
     * Страница стены пользователя (HTML)
     */
    @GetMapping("/{wallOwnerId}")
    public String getWallPage(@PathVariable Long wallOwnerId,
                              Authentication authentication,
                              Model model) {

        Long currentUserId = getCurrentUserId(authentication);

        // Проверяем доступ к стене
        if (!wallPostService.canAccessWall(wallOwnerId, currentUserId)) {
            return "redirect:/access-denied";
        }

        // Получаем информацию о владельце стены
        var wallOwner = userService.getUserById(wallOwnerId);

        model.addAttribute("wallOwner", wallOwner);
        model.addAttribute("wallOwnerId", wallOwnerId);
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("canWriteOnWall", wallPostService.canAccessWall(wallOwnerId, currentUserId));

        return "wall/wall-page"; // Thymeleaf шаблон
    }

    /**
     * REST API: Получение постов стены (для бесконечного скролла)
     */
    @GetMapping("/api/{wallOwnerId}/posts")
    @ResponseBody
    public ResponseEntity<WallPostPageResponse> getWallPosts(
            @PathVariable Long wallOwnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        WallPostPageResponse response = wallPostService.getWallPosts(
                wallOwnerId, currentUserId, page, size);

        return ResponseEntity.ok(response);
    }

    /**
     * REST API: Создание поста на стене
     */
    @PostMapping(value = "/api/{wallOwnerId}/posts",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<WallPostCardResponse> createWallPost(
            @PathVariable Long wallOwnerId,
            @ModelAttribute WallPostCreateRequest request,
            Authentication authentication) throws IOException {

        Long currentUserId = getCurrentUserId(authentication);

        // Устанавливаем владельца стены
        request.setWallOwnerId(wallOwnerId);

        WallPostCardResponse response = wallPostService.create(request, currentUserId);

        return ResponseEntity.ok(response);
    }

    /**
     * REST API: Удаление поста
     */
    @DeleteMapping("/api/posts/{postId}")
    @ResponseBody
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        wallPostService.delete(postId, currentUserId);

        return ResponseEntity.noContent().build();
    }

    /**
     * REST API: Получение конкретного поста
     */
    @GetMapping("/api/posts/{postId}")
    @ResponseBody
    public ResponseEntity<WallPostCardResponse> getPost(@PathVariable Long postId,
                                                        Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        WallPostCardResponse response = wallPostService.getById(postId, currentUserId);

        return ResponseEntity.ok(response);
    }

    /**
     * Получение ID текущего пользователя
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}

