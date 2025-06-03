package com.example.cleopatra.controller;


import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.PostService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/feed")
@RequiredArgsConstructor
@Slf4j

public class FeedController {

    private final PostService postService;
    private final UserService userService;

    /**
     * Главная лента новостей
     */
    @GetMapping
    public String showFeed(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           Model model,
                           Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            // Получаем текущего пользователя
            User currentUser = userService.getCurrentUserEntity();

            // Получаем ленту постов
            PostListDto feedPosts = postService.getFeedPosts(currentUser.getId(), page, size);

            model.addAttribute("posts", feedPosts);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            log.info("Отображение ленты для пользователя {}: {} постов на странице {}",
                    currentUser.getId(), feedPosts.getNumberOfElements(), page);

            return "feed/index";

        } catch (Exception e) {
            log.error("Ошибка при загрузке ленты: {}", e.getMessage());
            model.addAttribute("errorMessage", "Не удалось загрузить ленту новостей");
            return "error/500";
        }
    }

    /**
     * AJAX загрузка дополнительных постов
     */
    @GetMapping("/api/posts")
    @ResponseBody
    public ResponseEntity<PostListDto> getFeedPostsApi(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       Authentication authentication) {
        try {
            User currentUser = userService.getCurrentUserEntity();
            PostListDto feedPosts = postService.getFeedPosts(currentUser.getId(), page, size);
            return ResponseEntity.ok(feedPosts);
        } catch (Exception e) {
            log.error("Ошибка API загрузки ленты: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

