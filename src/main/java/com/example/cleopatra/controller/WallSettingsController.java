package com.example.cleopatra.controller;
import com.example.cleopatra.dto.WallPost.WallSettingsResponse;
import com.example.cleopatra.dto.WallPost.WallSettingsUpdateRequest;
import com.example.cleopatra.enums.WallAccessLevel;
import com.example.cleopatra.service.UserService;
import com.example.cleopatra.service.WallSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/wall/settings")
public class WallSettingsController {

    private final WallSettingsService wallSettingsService;
    private final UserService userService;

    /**
     * Страница настроек стены (HTML)
     */
    @GetMapping("/{userId}")
    public String getWallSettingsPage(@PathVariable Long userId,
                                      Authentication authentication,
                                      Model model) {

        Long currentUserId = getCurrentUserId(authentication);

        // Проверяем права доступа
        if (!wallSettingsService.canEditWallSettings(userId, currentUserId)) {
            return "redirect:/access-denied";
        }

        // Получаем пользователя и его настройки
        var user = userService.getUserById(userId);
        var settings = wallSettingsService.getWallSettings(userId);
        var allLevels = wallSettingsService.getAllAccessLevels();

        model.addAttribute("user", user);
        model.addAttribute("settings", settings);
        model.addAttribute("allAccessLevels", allLevels);
        model.addAttribute("currentUserId", currentUserId);

        return "wall/settings"; // Thymeleaf шаблон
    }

    /**
     * REST API: Получение настроек стены
     */
    @GetMapping("/api/{userId}")
    @ResponseBody
    public ResponseEntity<WallSettingsResponse> getWallSettings(
            @PathVariable Long userId,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        // Проверяем права просмотра настроек (только владелец)
        if (!wallSettingsService.canEditWallSettings(userId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        WallSettingsResponse settings = wallSettingsService.getWallSettings(userId);

        return ResponseEntity.ok(settings);
    }

    /**
     * REST API: Обновление уровня доступа к стене
     */
    @PatchMapping("/api/{userId}/access-level")
    @ResponseBody
    public ResponseEntity<WallSettingsResponse> updateWallAccessLevel(
            @PathVariable Long userId,
            @RequestParam WallAccessLevel accessLevel,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        try {
            WallSettingsResponse response = wallSettingsService.updateWallAccessLevel(
                    userId, accessLevel, currentUserId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка обновления уровня доступа к стене", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * REST API: Полное обновление настроек стены
     */
    @PutMapping("/api/{userId}")
    @ResponseBody
    public ResponseEntity<WallSettingsResponse> updateWallSettings(
            @PathVariable Long userId,
            @RequestBody @Valid WallSettingsUpdateRequest request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);

        try {
            WallSettingsResponse response = wallSettingsService.updateWallSettings(
                    userId, request, currentUserId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка обновления настроек стены", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * REST API: Получение всех доступных уровней доступа
     */
    @GetMapping("/api/access-levels")
    @ResponseBody
    public ResponseEntity<WallAccessLevel[]> getAllAccessLevels() {

        WallAccessLevel[] levels = wallSettingsService.getAllAccessLevels();

        return ResponseEntity.ok(levels);
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

