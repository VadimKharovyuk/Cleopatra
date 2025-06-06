package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.ExistsException.ImageValidationException;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileApiController {

    private final UserService userService;

    /**
     * REST API для загрузки аватара
     */
    @PostMapping("/{userId}/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatarApi(
            @PathVariable Long userId,
            @RequestParam("avatar") MultipartFile file) {

        try {
            UserResponse updatedUser = userService.uploadAvatar(userId, file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Аватар успешно обновлен",
                    "user", updatedUser
            ));

        } catch (ImageValidationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "validation",
                    "message", e.getMessage()
            ));

        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке аватара через API для пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "internal",
                    "message", "Ошибка сервера"
            ));
        }
    }

    /**
     * REST API для загрузки фона
     */
    @PostMapping("/{userId}/background")
    public ResponseEntity<Map<String, Object>> uploadBackgroundApi(
            @PathVariable Long userId,
            @RequestParam("background") MultipartFile file) {

        try {
            UserResponse updatedUser = userService.uploadBackgroundImage(userId, file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Фон успешно обновлен",
                    "user", updatedUser
            ));

        } catch (ImageValidationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "validation",
                    "message", e.getMessage()
            ));

        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке фона через API для пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "internal",
                    "message", "Ошибка сервера"
            ));
        }
    }


    @PostMapping("/me/notification-settings")
    public ResponseEntity<String> updateNotificationSettings(
            @RequestBody NotificationSettingsRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        Long userId = userService.getUserIdByEmail(email);

        userService.updateNotificationSettings(userId, request.getReceiveVisitNotifications());
        return ResponseEntity.ok("Settings updated");
    }

    @Data
    public static class NotificationSettingsRequest {
        private Boolean receiveVisitNotifications;
    }
}