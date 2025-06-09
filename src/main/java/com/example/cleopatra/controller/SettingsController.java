package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;

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

    @GetMapping
    public String Settings(Model model, Authentication authentication) {
        if (authentication != null) {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);
            UserResponse user = userService.getUserById(userId);

            model.addAttribute("userId", userId);
            model.addAttribute("user", user);
        }
        return "settings/dashboard";
    }



    // Новый метод для AJAX обновления приватности
    @PostMapping("/privacy/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> togglePrivacy(
            @RequestBody Map<String, Boolean> request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication != null) {
                String email = authentication.getName();
                Long userId = userService.getUserIdByEmail(email);
                Boolean isPrivate = request.get("isPrivateProfile");

                userService.updateProfilePrivacy(userId, isPrivate);

                response.put("success", true);
                response.put("message", isPrivate ? "Профиль стал приватным" : "Профиль стал публичным");

                log.info("User {} updated privacy setting to: {}", userId, isPrivate);
            } else {
                response.put("success", false);
                response.put("message", "Пользователь не авторизован");
            }
        } catch (Exception e) {
            log.error("Error updating privacy settings", e);
            response.put("success", false);
            response.put("message", "Ошибка при обновлении настроек");
        }

        return ResponseEntity.ok(response);
    }
}
