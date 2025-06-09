package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.ChangePasswordDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/security")
public class SecurityPasswordController {
    private final UserService userService;

    @GetMapping
    public String securityPassword(Model model,
                                   @RequestParam(required = false) String success,
                                   @AuthenticationPrincipal UserDetails userDetails) {

        model.addAttribute("changePasswordDto", new ChangePasswordDto());

        // Добавляем информацию о текущем пользователе
        addCurrentUserToModel(model, userDetails);

        if ("true".equals(success)) {
            model.addAttribute("successMessage", "Пароль успешно изменен");
        }

        return "security/changePassword";
    }

    private void addCurrentUserToModel(Model model, UserDetails userDetails) {
        if (userDetails != null) {
            try {
                UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("userId", currentUser.getId());
            } catch (Exception e) {
                log.warn("Не удалось получить информацию о пользователе: {}", userDetails.getUsername());
            }
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePasswordApi(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordDto changePasswordDto) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        userService.changePassword(userId, changePasswordDto);
        return ResponseEntity.ok("Пароль успешно изменен");
    }

    @PostMapping("/change-password-form")
    public String changePasswordForm(@Valid @ModelAttribute ChangePasswordDto changePasswordDto,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("changePasswordDto", changePasswordDto);
            return "security/changePassword";
        }

        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            userService.changePassword(userId, changePasswordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Пароль успешно изменен");
            return "redirect:/security?success=true";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("changePasswordDto", changePasswordDto);
            return "security/changePassword";
        }
    }
}
