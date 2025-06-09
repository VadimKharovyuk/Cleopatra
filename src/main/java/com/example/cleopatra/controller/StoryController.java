package com.example.cleopatra.controller;
import com.example.cleopatra.dto.StoryDTO.StoryDTO;
import com.example.cleopatra.dto.StoryDTO.StoryList;
import com.example.cleopatra.dto.StoryView.StoryViewDTO;
import com.example.cleopatra.enums.StoryEmoji;
import com.example.cleopatra.service.StoryService;
import com.example.cleopatra.service.StoryViewService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final StoryViewService storyViewService;
    private final UserService userService;


    /**
     * Главная страница историй
     */
    @GetMapping
    public String storiesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            Model model) {

        try {
            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);

            StoryList storyList = storyService.getAllActiveStories(page, size, currentUserId);
            model.addAttribute("storyList", storyList);
            model.addAttribute("currentUserId", currentUserId);
            model.addAttribute("storyEmojiValues", StoryEmoji.values());

            return "stories/stories";
        } catch (Exception e) {
            log.error("Error loading stories page", e);
            model.addAttribute("error", "Ошибка загрузки историй: " + e.getMessage());
            return "stories/stories";
        }
    }

    /**
     * Страница создания истории
     */
    @GetMapping("/create")
    public String createStoryPage(Authentication authentication, Model model) {

        String email = authentication.getName();
        Long userId = userService.getUserIdByEmail(email);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("storyEmojiValues", StoryEmoji.values());
        return "stories/create";
    }

    /**
     * Страница детального просмотра истории
     */
    @GetMapping("/{storyId}")
    public String storyDetailPage(
            @PathVariable Long storyId,
            Authentication authentication,
            Model model) {

        try {
            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);

            StoryDTO story = storyService.getStoryById(storyId, currentUserId);

            // Создаем просмотр, если пользователь еще не смотрел
            if (!story.getIsViewedByCurrentUser() && !story.getIsOwner()) {
                storyViewService.createView(storyId, currentUserId);
                // Обновляем данные после создания просмотра
                story = storyService.getStoryById(storyId, currentUserId);
            }

            // Получаем список просмотров
            List<StoryViewDTO> views = storyViewService.getStoryViews(storyId);

            model.addAttribute("story", story);
            model.addAttribute("views", views);
            model.addAttribute("currentUserId", currentUserId);
            model.addAttribute("storyEmojiValues", StoryEmoji.values());

            return "stories/detail";
        } catch (Exception e) {
            log.error("Error loading story detail", e);
            model.addAttribute("error", "Ошибка загрузки истории: " + e.getMessage());
            return "stories/stories";
        }
    }

    /**
     * Страница историй пользователя
     */
    @GetMapping("/user/{userId}")
    public String userStoriesPage(
            @PathVariable Long userId,
            Authentication authentication,
            Model model) {

        try {
            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);

            StoryList userStories = storyService.getUserStories(userId, currentUserId);
            model.addAttribute("userStories", userStories);
            model.addAttribute("userId", userId);
            model.addAttribute("currentUserId", currentUserId);

            return "stories/user-stories";
        } catch (Exception e) {
            log.error("Error loading user stories", e);
            model.addAttribute("error", "Ошибка загрузки историй пользователя: " + e.getMessage());
            return "stories/stories";
        }
    }

    // ===== REST API =====

    /**
     * Создать новую историю
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<?> createStory(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) StoryEmoji emoji,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);

            StoryDTO story = storyService.createStory(currentUserId, file, emoji, description);
            return ResponseEntity.ok(story);
        } catch (IOException e) {
            log.error("Error creating story", e);
            return ResponseEntity.badRequest().body("Ошибка загрузки файла: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error creating story", e);
            return ResponseEntity.badRequest().body("Ошибка создания истории: " + e.getMessage());
        }
    }

    /**
     * Получить изображение истории
     */
    @GetMapping("/image/{imageId}")
    public ResponseEntity<byte[]> getStoryImage(@PathVariable String imageId) {
        try {
            byte[] imageData = storyService.getStoryImage(imageId);
            String contentType = storyService.getImageContentType(imageId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(imageData.length);
            headers.setCacheControl("max-age=3600"); // Кэшируем на час

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error getting story image: {}", imageId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Обновить эмодзи истории
     */
    @PutMapping("/api/{storyId}/emoji")
    @ResponseBody
    public ResponseEntity<?> updateStoryEmoji(
            @PathVariable Long storyId,
            @RequestParam StoryEmoji emoji,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);

            StoryDTO story = storyService.updateStoryEmoji(storyId, currentUserId, emoji);
            return ResponseEntity.ok(story);
        } catch (Exception e) {
            log.error("Error updating story emoji", e);
            return ResponseEntity.badRequest().body("Ошибка обновления эмодзи: " + e.getMessage());
        }
    }

    /**
     * Удалить историю
     */
    @DeleteMapping("/api/{storyId}")
    @ResponseBody
    public ResponseEntity<?> deleteStory(
            @PathVariable Long storyId,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);

            boolean deleted = storyService.deleteStory(storyId, currentUserId);
            if (deleted) {
                return ResponseEntity.ok().body("История удалена успешно");
            } else {
                return ResponseEntity.badRequest().body("Не удалось удалить историю");
            }
        } catch (Exception e) {
            log.error("Error deleting story", e);
            return ResponseEntity.badRequest().body("Ошибка удаления истории: " + e.getMessage());
        }
    }

    /**
     * Получить просмотры истории (AJAX)
     */
    @GetMapping("/api/{storyId}/views")
    @ResponseBody
    public ResponseEntity<List<StoryViewDTO>> getStoryViews(@PathVariable Long storyId) {
        try {
            List<StoryViewDTO> views = storyViewService.getStoryViews(storyId);
            return ResponseEntity.ok(views);
        } catch (Exception e) {
            log.error("Error getting story views", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Создать просмотр истории (AJAX)
     */
    @PostMapping("/api/{storyId}/view")
    @ResponseBody
    public ResponseEntity<?> createStoryView(
            @PathVariable Long storyId,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);

            StoryViewDTO view = storyViewService.createView(storyId, currentUserId);
            return ResponseEntity.ok(view);
        } catch (Exception e) {
            log.error("Error creating story view", e);
            return ResponseEntity.badRequest().body("Ошибка создания просмотра: " + e.getMessage());
        }
    }
}