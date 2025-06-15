package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;

import com.example.cleopatra.enums.ProfileAccessLevel;
import com.example.cleopatra.service.ProfileAccessService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/settings")
public class SettingsController {
    private final UserService userService;
    private final ProfileAccessService profileAccessService;


    @GetMapping
    public String settings(Model model) {

        return "settings/dashboard";
    }

    @GetMapping("/privacy")
    public String Privacy(Model model, Authentication authentication) {
        if (authentication != null) {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            // Получаем текущие настройки приватности пользователя
            ProfileAccessLevel profileLevel = profileAccessService.getProfileAccessLevel(userId);
            ProfileAccessLevel photosLevel = profileAccessService.getPhotosAccessLevel(userId);
            ProfileAccessLevel postsLevel = profileAccessService.getPostsAccessLevel(userId);

            // Передаем все необходимые данные в модель
            model.addAttribute("profileAccessLevel", profileLevel);
            model.addAttribute("photosAccessLevel", photosLevel);
            model.addAttribute("postsAccessLevel", postsLevel);
            model.addAttribute("accessLevels", ProfileAccessLevel.values());
            model.addAttribute("userId", userId);
        }

        return "settings/privacy";
    }

    // Новый метод для AJAX обновления приватности профиля
    @PostMapping("/privacy/profile")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfilePrivacy(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication != null) {
                String email = authentication.getName();
                Long userId = userService.getUserIdByEmail(email);

                // Получаем новый уровень доступа из запроса
                String accessLevelStr = request.get("accessLevel");
                ProfileAccessLevel accessLevel = ProfileAccessLevel.valueOf(accessLevelStr.toUpperCase());

                // Обновляем через новый сервис
                boolean success = profileAccessService.updateProfileAccessLevel(userId, userId, accessLevel);

                if (success) {
                    response.put("success", true);
                    response.put("message", "Настройки приватности профиля обновлены");
                    response.put("newAccessLevel", accessLevel.name());
                    response.put("displayName", accessLevel.getDisplayName());

                    log.info("User {} updated profile privacy to: {}", userId, accessLevel);
                } else {
                    response.put("success", false);
                    response.put("message", "Не удалось обновить настройки");
                }
            } else {
                response.put("success", false);
                response.put("message", "Пользователь не авторизован");
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid access level provided", e);
            response.put("success", false);
            response.put("message", "Неверный уровень доступа");
        } catch (Exception e) {
            log.error("Error updating profile privacy settings", e);
            response.put("success", false);
            response.put("message", "Ошибка при обновлении настроек");
        }

        return ResponseEntity.ok(response);
    }

    // Новый метод для обновления приватности фото
    @PostMapping("/privacy/photos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePhotosPrivacy(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication != null) {
                String email = authentication.getName();
                Long userId = userService.getUserIdByEmail(email);

                String accessLevelStr = request.get("accessLevel");
                ProfileAccessLevel accessLevel = ProfileAccessLevel.valueOf(accessLevelStr.toUpperCase());

                boolean success = profileAccessService.updatePhotosAccessLevel(userId, userId, accessLevel);

                if (success) {
                    response.put("success", true);
                    response.put("message", "Настройки приватности фото обновлены");
                    response.put("newAccessLevel", accessLevel.name());
                    response.put("displayName", accessLevel.getDisplayName());

                    log.info("User {} updated photos privacy to: {}", userId, accessLevel);
                } else {
                    response.put("success", false);
                    response.put("message", "Не удалось обновить настройки");
                }
            } else {
                response.put("success", false);
                response.put("message", "Пользователь не авторизован");
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid access level provided for photos", e);
            response.put("success", false);
            response.put("message", "Неверный уровень доступа");
        } catch (Exception e) {
            log.error("Error updating photos privacy settings", e);
            response.put("success", false);
            response.put("message", "Ошибка при обновлении настроек");
        }

        return ResponseEntity.ok(response);
    }

    // Новый метод для обновления приватности постов
    @PostMapping("/privacy/posts")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePostsPrivacy(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication != null) {
                String email = authentication.getName();
                Long userId = userService.getUserIdByEmail(email);

                String accessLevelStr = request.get("accessLevel");
                ProfileAccessLevel accessLevel = ProfileAccessLevel.valueOf(accessLevelStr.toUpperCase());

                boolean success = profileAccessService.updatePostsAccessLevel(userId, userId, accessLevel);

                if (success) {
                    response.put("success", true);
                    response.put("message", "Настройки приватности постов обновлены");
                    response.put("newAccessLevel", accessLevel.name());
                    response.put("displayName", accessLevel.getDisplayName());

                    log.info("User {} updated posts privacy to: {}", userId, accessLevel);
                } else {
                    response.put("success", false);
                    response.put("message", "Не удалось обновить настройки");
                }
            } else {
                response.put("success", false);
                response.put("message", "Пользователь не авторизован");
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid access level provided for posts", e);
            response.put("success", false);
            response.put("message", "Неверный уровень доступа");
        } catch (Exception e) {
            log.error("Error updating posts privacy settings", e);
            response.put("success", false);
            response.put("message", "Ошибка при обновлении настроек");
        }

        return ResponseEntity.ok(response);
    }

    // Метод для получения текущих настроек приватности
    @GetMapping("/privacy/settings")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPrivacySettings(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication != null) {
                String email = authentication.getName();
                Long userId = userService.getUserIdByEmail(email);

                ProfileAccessLevel profileLevel = profileAccessService.getProfileAccessLevel(userId);
                ProfileAccessLevel photosLevel = profileAccessService.getPhotosAccessLevel(userId);
                ProfileAccessLevel postsLevel = profileAccessService.getPostsAccessLevel(userId);

                Map<String, Object> settings = new HashMap<>();
                settings.put("profile", Map.of(
                        "level", profileLevel.name(),
                        "displayName", profileLevel.getDisplayName(),
                        "description", profileLevel.getDescription()
                ));
                settings.put("photos", Map.of(
                        "level", photosLevel.name(),
                        "displayName", photosLevel.getDisplayName(),
                        "description", photosLevel.getDescription()
                ));
                settings.put("posts", Map.of(
                        "level", postsLevel.name(),
                        "displayName", postsLevel.getDisplayName(),
                        "description", postsLevel.getDescription()
                ));

                response.put("success", true);
                response.put("settings", settings);
            } else {
                response.put("success", false);
                response.put("message", "Пользователь не авторизован");
            }
        } catch (Exception e) {
            log.error("Error getting privacy settings", e);
            response.put("success", false);
            response.put("message", "Ошибка при получении настроек");
        }

        return ResponseEntity.ok(response);
    }
}
