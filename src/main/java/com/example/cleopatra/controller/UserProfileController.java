package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;

    /**
     * Отображает страницу профиля пользователя
     */
    @GetMapping("/{userId}")
    public String showProfile(@PathVariable Long userId, Model model) {
        try {
            UserResponse user = userService.getUserById(userId);
            model.addAttribute("user", user);
            model.addAttribute("updateProfileDto", new UpdateProfileDto());
            return "profile/profile";
        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке профиля пользователя {}: {}", userId, e.getMessage());
            model.addAttribute("error", "Пользователь не найден");
            return "error/404";
        }
    }

    /**
     * Отображает страницу редактирования профиля
     */
    @GetMapping("/{userId}/edit")
    public String showEditProfile(@PathVariable Long userId, Model model) {
        try {
            UserResponse user = userService.getUserById(userId);

            // Заполняем DTO текущими данными
            UpdateProfileDto dto = new UpdateProfileDto();
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());

            model.addAttribute("user", user);
            model.addAttribute("updateProfileDto", dto);
            return "profile/edit";
        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке страницы редактирования профиля {}: {}", userId, e.getMessage());
            model.addAttribute("error", "Пользователь не найден");
            return "error/404";
        }
    }

    /**
     * Обрабатывает обновление профиля
     */
    @PostMapping("/{userId}/update")
    public String updateProfile(
            @PathVariable Long userId,
            @Valid @ModelAttribute UpdateProfileDto updateProfileDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            try {
                UserResponse user = userService.getUserById(userId);
                model.addAttribute("user", user);
                model.addAttribute("updateProfileDto", updateProfileDto);
                return "profile/edit";
            } catch (RuntimeException e) {
                model.addAttribute("error", "Пользователь не найден");
                return "error/404";
            }
        }

        try {
            userService.updateProfile(userId, updateProfileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен!");
            return "redirect:/profile/" + userId;
        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении профиля {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении профиля: " + e.getMessage());
            return "redirect:/profile/" + userId + "/edit";
        }
    }

    /**
     * Обрабатывает загрузку аватара
     */
    @PostMapping("/{userId}/avatar/upload")
    public String uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("avatarFile") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пожалуйста, выберите файл для загрузки");
            return "redirect:/profile/" + userId + "/edit";
        }

        try {
            userService.uploadAvatar(userId, file);
            redirectAttributes.addFlashAttribute("successMessage", "Аватар успешно загружен!");
        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке аватара для пользователя {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке аватара: " + e.getMessage());
        }

        return "redirect:/profile/" + userId;
    }

    /**
     * Удаляет аватар пользователя
     */
    @PostMapping("/{userId}/avatar/delete")
    public String deleteAvatar(
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {

        try {
            userService.deleteAvatar(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Аватар успешно удален!");
        } catch (RuntimeException e) {
            log.error("Ошибка при удалении аватара для пользователя {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении аватара: " + e.getMessage());
        }

        return "redirect:/profile/" + userId;
    }
}