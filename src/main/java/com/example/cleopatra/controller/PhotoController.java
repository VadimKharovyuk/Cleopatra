package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.PhotoCreateDto;
import com.example.cleopatra.dto.user.PhotoResponseDto;
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
    @GetMapping
    public String photosPage(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Long userId = getCurrentUserId(authentication);

        // Проверяем, может ли пользователь загружать фото
        boolean canUpload = photoService.canUploadPhoto(userId);
        model.addAttribute("canUpload", canUpload);
        model.addAttribute("photoCreateDto", new PhotoCreateDto());

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

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}