package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.PhotoCreateDto;
import com.example.cleopatra.dto.user.PhotoResponseDto;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.PhotoService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/photos")
@RequiredArgsConstructor
@Slf4j
public class PhotoController {

    private final PhotoService photoService;
    private final UserService userService;


    // Главная страница с фотографиями
// Главная страница с фотографиями
    @GetMapping
    public String photosPage(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Long userId = getCurrentUserId(authentication);

        // Проверяем, может ли пользователь загружать фото
        boolean canUpload = photoService.canUploadPhoto(userId);

        // Получаем информацию о квоте
        int totalLimit = photoService.getPhotoLimitForUserId(userId);
        int remainingLimit = photoService.getRemainingPhotoLimit(userId);
        int usedCount = totalLimit - remainingLimit;

        // Рассчитываем процент использования
        int usagePercentage = totalLimit > 0 ? Math.round((usedCount * 100.0f) / totalLimit) : 0;
        double strokeDashoffset = 314.16 - (totalLimit > 0 ? (usedCount * 314.16 / totalLimit) : 0);

        model.addAttribute("canUpload", canUpload);
        model.addAttribute("photoCreateDto", new PhotoCreateDto());

        // Добавляем информацию о лимитах
        model.addAttribute("totalLimit", totalLimit);
        model.addAttribute("remainingLimit", remainingLimit);
        model.addAttribute("usedCount", usedCount);
        model.addAttribute("usagePercentage", usagePercentage);
        model.addAttribute("strokeDashoffset", strokeDashoffset);

        return "photos/index";
    }

    // Загрузка нового фото
    @PostMapping("/upload")
    public String uploadPhoto(@ModelAttribute PhotoCreateDto photoCreateDto,
                            Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            Long userId = getCurrentUserId(authentication);
            PhotoResponseDto response = photoService.createPhoto(userId, photoCreateDto);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Фото успешно загружено! ID: " + response.getPicId());

        } catch (Exception e) {
            log.error("Error uploading photo: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка загрузки фото: " + e.getMessage());
        }

        return "redirect:/photos";
    }

    // Просмотр конкретного фото
    @GetMapping("/{photoId}")
    public String viewPhoto(@PathVariable Long photoId,
                            Model model,
                           Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            Long userId = getCurrentUserId(authentication);
            PhotoResponseDto photo = photoService.getPhotoById(userId, photoId);
            model.addAttribute("photo", photo);
            return "photos/view";

        } catch (Exception e) {
            log.error("Error viewing photo {}: {}", photoId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Фото не найдено: " + e.getMessage());
            return "photos/index";
        }
    }

    // Удаление фото
    @PostMapping("/{photoId}/delete")
    public String deletePhoto(@PathVariable Long photoId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            Long userId = getCurrentUserId(authentication);
            photoService.deletePhoto(userId, photoId);

            redirectAttributes.addFlashAttribute("successMessage", "Фото успешно удалено!");

        } catch (Exception e) {
            log.error("Error deleting photo {}: {}", photoId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка удаления фото: " + e.getMessage());
        }

        return "redirect:/photos";
    }

    // REST API endpoints для AJAX

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<?> uploadPhotoApi(@RequestParam("image") MultipartFile image,
                                            @RequestParam(value = "description", required = false) String description,
                                            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            Long userId = getCurrentUserId(authentication);

            PhotoCreateDto photoCreateDto = new PhotoCreateDto();
            photoCreateDto.setImage(image);
            photoCreateDto.setDescription(description);

            PhotoResponseDto response = photoService.createPhoto(userId, photoCreateDto);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("API Error uploading photo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/{photoId}")
    @ResponseBody
    public ResponseEntity<?> deletePhotoApi(@PathVariable Long photoId, Authentication  authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            Long userId = getCurrentUserId(authentication);
            photoService.deletePhoto(userId, photoId);
            return ResponseEntity.ok("Photo deleted successfully");

        } catch (Exception e) {
            log.error("API Error deleting photo {}: {}", photoId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/api/{photoId}")
    @ResponseBody
    public ResponseEntity<?> getPhotoApi(@PathVariable Long photoId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            Long userId = getCurrentUserId(authentication);
            PhotoResponseDto photo = photoService.getPhotoById(userId, photoId);
            return ResponseEntity.ok(photo);

        } catch (Exception e) {
            log.error("API Error getting photo {}: {}", photoId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }


    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<PhotoResponseDto>> getUserPhotos(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Long userId = getCurrentUserId(authentication);
            List<PhotoResponseDto> photos = photoService.getUserPhotos(userId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            log.error("API Error getting user photos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }



    // Просмотр фотографий другого пользователя
    @GetMapping("/user/{userId}")
    public String viewUserPhotos(@PathVariable Long userId,
                                 Model model,
                                 Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // Получаем информацию о пользователе
            User user = userService.findById(userId);
            if (user == null) {
                model.addAttribute("errorMessage", "Пользователь не найден");
                return "error/404";
            }

            // Получаем фото пользователя
            List<PhotoResponseDto> photos = photoService.getPublicPhotos(userId);

            model.addAttribute("user", user);
            model.addAttribute("photos", photos);
            model.addAttribute("isOwner", userId.equals(getCurrentUserId(authentication)));

            return "photos/user-gallery";

        } catch (Exception e) {
            log.error("Error viewing user photos {}: {}", userId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Ошибка загрузки фотографий: " + e.getMessage());
            return "error/500";
        }
    }

    // API для получения фотографий пользователя
    @GetMapping("/api/user/{userId}")
    @ResponseBody
    public ResponseEntity<List<PhotoResponseDto>> getUserPhotosApi(@PathVariable Long userId,
                                                                   Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<PhotoResponseDto> photos = photoService.getPublicPhotos(userId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            log.error("API Error getting user photos {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Просмотр конкретного фото другого пользователя
    @GetMapping("/user/{userId}/photo/{photoId}")
    public String viewUserPhoto(@PathVariable Long userId,
                                @PathVariable Long photoId,
                                Model model,
                                Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            PhotoResponseDto photo = photoService.getPublicPhotoById(photoId);

            // Проверяем, что фото принадлежит указанному пользователю
            if (!photo.getAuthorId().equals(userId)) {
                model.addAttribute("errorMessage", "Фото не найдено");
                return "error/404";
            }

            User user = userService.findById(userId);

            model.addAttribute("photo", photo);
            model.addAttribute("user", user);
            model.addAttribute("isOwner", userId.equals(getCurrentUserId(authentication)));

            return "photos/user-photo-view";

        } catch (Exception e) {
            log.error("Error viewing user photo {}/{}: {}", userId, photoId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Фото не найдено: " + e.getMessage());
            return "photos/user-gallery";
        }
    }


    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}