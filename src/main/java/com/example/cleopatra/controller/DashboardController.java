package com.example.cleopatra.controller;

import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.PostService;
import com.example.cleopatra.service.SubscriptionService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.attribute.UserPrincipal;
@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/dashboard")
public class DashboardController {
    private final UserService userService;
    private final PostService postService;




    @GetMapping
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("postCreateDto", new PostCreateDto());

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                User currentUser = userService.getCurrentUserEntity();

                // Добавляем несколько последних постов из ленты на главную (показываем только 3-5)
                PostListDto recentFeedPosts = postService.getFeedPosts(currentUser.getId(), 0, 10);
                model.addAttribute("recentPosts", recentFeedPosts);
                model.addAttribute("currentUser", currentUser);



                // Дополнительно можно добавить статистику для dashboard
                // model.addAttribute("totalFollowers", userService.getFollowersCount(currentUser.getId()));
                // model.addAttribute("totalFollowing", userService.getFollowingCount(currentUser.getId()));

            } catch (Exception e) {
                log.warn("Не удалось загрузить данные для dashboard: {}", e.getMessage());
                // Добавляем пустой объект, чтобы избежать ошибок в шаблоне
                model.addAttribute("recentPosts", new PostListDto());
            }
        }

        return "user/dashboard";
    }

}
