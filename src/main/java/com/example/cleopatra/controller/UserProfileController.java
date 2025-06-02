package com.example.cleopatra.controller;

import com.example.cleopatra.ExistsException.ImageValidationException;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserRecommendationDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.ImageValidator;
import com.example.cleopatra.service.RecommendationService;
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

import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;
    private final RecommendationService recommendationService;
    private final ImageValidator imageValidator;



    @GetMapping("/{userId}")
    public String showProfile(@PathVariable Long userId, Model model) {
        try {
            UserResponse user = userService.getUserById(userId);
            model.addAttribute("user", user);



            model.addAttribute("updateProfileDto", new UpdateProfileDto());


            List<UserRecommendationDto> recommendations =recommendationService.getTopRecommendations(userId);
            model.addAttribute("recommendations", recommendations);


            return "profile/profile";
        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке профиля пользователя {}: {}", userId, e.getMessage());
            model.addAttribute("error", "Пользователь не найден");
            return "error/404";
        }
    }

    @GetMapping("/{userId}/edit")
    public String showEditProfile(@PathVariable Long userId, Model model) {
        try {
            UserResponse user = userService.getUserById(userId);

            UpdateProfileDto dto = new UpdateProfileDto();
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());

            model.addAttribute("user", user);
            model.addAttribute("updateProfileDto", dto);

            // ✅ Добавляем настройки валидации
            model.addAttribute("maxFileSize", imageValidator.getMaxFileSizeMB());
            model.addAttribute("allowedFormats", imageValidator.getAllowedExtensions());
            model.addAttribute("validationRules", imageValidator.getValidationRulesDescription());

            return "profile/edit";
        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке страницы редактирования профиля {}: {}", userId, e.getMessage());
            model.addAttribute("error", "Пользователь не найден");
            return "error/404";
        }
    }

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

                // Не забываем добавить настройки валидации при ошибках
                model.addAttribute("maxFileSize", imageValidator.getMaxFileSizeMB());
                model.addAttribute("allowedFormats", imageValidator.getAllowedExtensions());
                model.addAttribute("validationRules", imageValidator.getValidationRulesDescription());


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
     * ✅ ИСПРАВЛЕННЫЙ маршрут для загрузки аватара
     * URL: /profile/{userId}/avatar (БЕЗ /upload)
     * Параметр: avatar (НЕ avatarFile)
     */
    @PostMapping("/{userId}/avatar")
    public String uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("avatar") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        log.info("📤 Загрузка аватара для пользователя {}: {}", userId, file.getOriginalFilename());

        // ✅ Минимальная проверка в контроллере
        if (file.isEmpty()) {
            log.warn("⚠️ Пустой файл аватара для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("errorMessage", "Пожалуйста, выберите файл");
            return "redirect:/profile/" + userId + "/edit";
        }

        try {
            // ✅ Основная валидация и бизнес-логика В СЕРВИСЕ
            UserResponse updatedUser = userService.uploadAvatar(userId, file);

            log.info("✅ Аватар успешно загружен для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Аватар успешно обновлен!");
            return "redirect:/profile/" + userId;

        } catch (ImageValidationException e) {
            // Специфичные ошибки валидации изображений
            log.warn("❌ Ошибка валидации аватара для пользователя {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/profile/" + userId + "/edit";

        } catch (RuntimeException e) {
            // Общие ошибки
            log.error("❌ Ошибка при загрузке аватара для пользователя {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке аватара. Попробуйте позже.");
            return "redirect:/profile/" + userId + "/edit";
        }
    }

    /**
     * ✅ КОРРЕКТНЫЙ маршрут для загрузки фона
     * URL: /profile/{userId}/background/upload (КАК В HTML)
     * Параметр: backgroundFile (КАК В HTML)
     */
    @PostMapping("/{userId}/background/upload")
    public String uploadBackgroundImage(
            @PathVariable Long userId,
            @RequestParam("backgroundFile") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        log.info("📤 Загрузка фона для пользователя {}: {}", userId, file.getOriginalFilename());

        if (file.isEmpty()) {
            log.warn("⚠️ Пустой файл фона для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("errorMessage", "Пожалуйста, выберите файл для загрузки");
            return "redirect:/profile/" + userId + "/edit";
        }

        try {
            userService.uploadBackgroundImage(userId, file);

            log.info("✅ Фон успешно загружен для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Фоновое изображение успешно загружено!");
            return "redirect:/profile/" + userId;

        } catch (ImageValidationException e) {
            // Специфичные ошибки валидации изображений
            log.warn("❌ Ошибка валидации фона для пользователя {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/profile/" + userId + "/edit";

        } catch (RuntimeException e) {
            log.error("❌ Ошибка при загрузке фона для пользователя {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке фонового изображения: " + e.getMessage());
            return "redirect:/profile/" + userId + "/edit";
        }
    }

    /**
     * ✅ Удаление аватара
     */
    @PostMapping("/{userId}/avatar/delete")
    public String deleteAvatar(
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {

        log.info("🗑️ Удаление аватара для пользователя {}", userId);

        try {
            userService.deleteAvatar(userId);

            log.info("✅ Аватар успешно удален для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Фотография профиля удалена!");
            return "redirect:/profile/" + userId;

        } catch (RuntimeException e) {
            log.error("❌ Ошибка при удалении аватара для пользователя {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении фотографии профиля");
            return "redirect:/profile/" + userId + "/edit";
        }
    }

    /**
     * ✅ Удаление фона
     */
    @PostMapping("/{userId}/background/delete")
    public String deleteBackground(
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {

        log.info("🗑️ Удаление фона для пользователя {}", userId);

        try {
            userService.deleteBackgroundImage(userId);

            log.info("✅ Фон успешно удален для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Фоновое изображение удалено!");
            return "redirect:/profile/" + userId;

        } catch (RuntimeException e) {
            log.error("❌ Ошибка при удалении фона для пользователя {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении фонового изображения");
            return "redirect:/profile/" + userId + "/edit";
        }
    }

    // ========================================
    // ДОПОЛНИТЕЛЬНЫЕ УТИЛИТАРНЫЕ МЕТОДЫ
    // ========================================



    /**
     * Добавление настроек валидации в модель
     */
    private void addValidationAttributesToModel(Model model) {
        model.addAttribute("maxFileSize", imageValidator.getMaxFileSizeMB());
        model.addAttribute("allowedFormats", imageValidator.getAllowedExtensions());
        model.addAttribute("validationRules", imageValidator.getValidationRulesDescription());
    }




//    // Для AJAX запросов
//    @PostMapping("/api/users/{userId}/follow")
//    @ResponseBody
//    public Map<String, Object> followUser(@PathVariable Long userId) {
//        // Логика подписки
//        return Map.of("success", true, "message", "Вы подписались на пользователя");
//    }
//
//    @PostMapping("/api/users/{userId}/unfollow")
//    @ResponseBody
//    public Map<String, Object> unfollowUser(@PathVariable Long userId) {
//        // Логика отписки
//        return Map.of("success", true, "message", "Вы отписались от пользователя");
//    }
//
//    @PostMapping("/api/users/{userId}/block")
//    @ResponseBody
//    public Map<String, Object> blockUser(@PathVariable Long userId) {
//        // Логика блокировки
//        return Map.of("success", true, "message", "Пользователь заблокирован");
//    }
}