package com.example.cleopatra.controller;

import com.example.cleopatra.dto.StoryDTO.StoryDTO;
import com.example.cleopatra.dto.StoryView.StoryViewDTO;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.StoryService;
import com.example.cleopatra.service.StoryViewService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/views")
@RequiredArgsConstructor
public class StoryViewController {

    private final StoryViewService storyViewService;
    private final StoryService storyService;
    private final UserService userService;



    /**
     * Страница для просмотра списка просмотров истории
     * Только автор истории может просматривать свои просмотры
     */
    @GetMapping("/{storyId}/views")
    public String getStoryViews(@PathVariable Long storyId,
                                Authentication authentication,
                                Model model) {
        try {
            log.info("Requesting views for story: {}", storyId);

            // Получаем текущего пользователя
            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);


            UserResponse currentUser = userService.getUserById(currentUserId);

            // Получаем информацию об истории
            StoryDTO story = storyService.getStoryById(storyId, currentUserId);

            story.setImageUrl(currentUser.getImageUrl());
            story.setUserFirstName(currentUser.getFirstName());
            story.setUserLastName(currentUser.getLastName());

            // Проверяем, является ли текущий пользователь автором истории
            if (!story.getUserId().equals(currentUserId)) {
                log.warn("User {} tried to access views for story {} (not owner)", currentUserId, storyId);
                model.addAttribute("error", "Только автор истории может просматривать список просмотров");
                return "error";
            }


            // Получаем просмотры
            List<StoryViewDTO> views = storyViewService.getStoryViews(storyId);
            Long totalViews = storyViewService.getViewsCount(storyId);

            model.addAttribute("story", story);
            model.addAttribute("storyId", storyId);
            model.addAttribute("views", views);
            model.addAttribute("totalViews", totalViews);
            model.addAttribute("hasViews", !views.isEmpty());
            model.addAttribute("currentUserId", currentUserId);

            return "stories/story-views";

        } catch (IllegalArgumentException e) {
            log.warn("Story not found: {}", e.getMessage());
            model.addAttribute("error", "История не найдена");
            return "error";
        } catch (IllegalStateException e) {
            log.warn("Story expired: {}", e.getMessage());
            model.addAttribute("error", "История истекла и недоступна");
            return "error";
        } catch (Exception e) {
            log.error("Error getting story views", e);
            model.addAttribute("error", "Произошла ошибка при загрузке просмотров");
            return "error";
        }
    }
}
