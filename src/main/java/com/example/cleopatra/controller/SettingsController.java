package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;

import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
            Long userId = userService.getUserIdByEmail(email); // Получаем сразу ID
            model.addAttribute("userId", userId);

        }
        return "settings/dashboard";
    }
}
