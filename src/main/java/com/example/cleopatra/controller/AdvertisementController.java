package com.example.cleopatra.controller;

import com.example.cleopatra.dto.AdvertisementDTO.*;
import com.example.cleopatra.enums.AdCategory;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.AdvertisementListService;
import com.example.cleopatra.service.AdvertisementService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/advertisements")
@RequiredArgsConstructor
@Slf4j
public class AdvertisementController {

    private final AdvertisementService advertisementService;
    private final AdvertisementListService advertisementListService;
    private final UserService userService;

    // === СТРАНИЦЫ ДЛЯ ПОЛЬЗОВАТЕЛЕЙ ===

    /**
     * Список реклам пользователя
     */
    @GetMapping("/my")
    public String myAdvertisements(Authentication authentication ,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {

        String email = authentication.getName();
        Long user = userService.getUserIdByEmail(email);
        Pageable pageable = PageRequest.of(page, size);
        AdvertisementList adList = advertisementListService.getUserAds(user, pageable);

        model.addAttribute("advertisementList", adList);
        model.addAttribute("currentPage", "my-ads");
        return "advertisements/my-list";
    }

    /**
     * Форма создания рекламы
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("createDto", new CreateAdvertisementDTO());
        model.addAttribute("categories", AdCategory.values());
        model.addAttribute("genders", Gender.values());
        return "advertisements/create";
    }

    @PostMapping("/create")
    public String createAdvertisement(@Valid @ModelAttribute("createDto") CreateAdvertisementDTO dto,
                                      BindingResult result,
                                      @RequestParam(value = "image", required = false) MultipartFile image,
                                      Authentication authentication, // Добавляем параметр
                                      Model model,
                                      RedirectAttributes redirectAttributes) {

        // Получаем пользователя через ваш метод с параметром
        User user = userService.getCurrentUserEntity(authentication) ;
        if (user == null) {
            log.error("Пользователь не найден через getCurrentUserEntity(authentication)");
            redirectAttributes.addFlashAttribute("errorMessage", "Необходимо войти в систему");
            return "redirect:/login";
        }

        log.debug("Создание рекламы для пользователя: {}", user.getEmail());

        if (result.hasErrors()) {
            model.addAttribute("categories", AdCategory.values());
            model.addAttribute("genders", Gender.values());
            return "advertisements/create";
        }

        try {
            AdvertisementDetailDTO createdAd;
            if (image != null && !image.isEmpty()) {
                createdAd = advertisementService.createAdvertisement(dto, image, user);
            } else {
                createdAd = advertisementService.createAdvertisement(dto, user);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Реклама успешно создана! Ожидает модерации.");
            return "redirect:/advertisements/details/" + createdAd.getId();

        } catch (IOException e) {
            log.error("Ошибка загрузки изображения: {}", e.getMessage());
            result.reject("image.upload.error", "Ошибка загрузки изображения: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка создания рекламы: {}", e.getMessage());
            result.reject("advertisement.create.error", "Ошибка создания рекламы: " + e.getMessage());
        }

        model.addAttribute("categories", AdCategory.values());
        model.addAttribute("genders", Gender.values());
        return "advertisements/create";
    }


    /**
     * Детали рекламы
     */
    @GetMapping("/details/{id}")
    public String advertisementDetails(@PathVariable Long id,
                                      Authentication authentication,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

      User user = userService.getCurrentUserEntity(authentication);
        Optional<AdvertisementDetailDTO> adOptional = advertisementService.getAdvertisementDetails(id, user);

        if (adOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Реклама не найдена или у вас нет прав для просмотра");
            return "redirect:/advertisements/my";
        }

        model.addAttribute("advertisement", adOptional.get());
        model.addAttribute("isOwner", advertisementService.isOwner(id, user));
        return "advertisements/details";
    }

    /**
     * Форма редактирования рекламы
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal User user,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Optional<AdvertisementDetailDTO> adOptional = advertisementService.getAdvertisementDetails(id, user);

        if (adOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Реклама не найдена или у вас нет прав для редактирования");
            return "redirect:/advertisements/my";
        }

        AdvertisementDetailDTO ad = adOptional.get();

        // Проверяем права на редактирование
        if (!advertisementService.isOwner(id, user) && user.getRole() != com.example.cleopatra.enums.Role.ADMIN) {
            redirectAttributes.addFlashAttribute("errorMessage", "У вас нет прав для редактирования этой рекламы");
            return "redirect:/advertisements/details/" + id;
        }

        UpdateAdvertisementDTO updateDto = UpdateAdvertisementDTO.builder()
                .title(ad.getTitle())
                .description(ad.getDescription())
                .shortDescription(ad.getShortDescription())
                .url(ad.getUrl())
                .startTime(ad.getStartTime())
                .endTime(ad.getEndTime())
                .endDate(ad.getEndDate())
                .build();

        model.addAttribute("updateDto", updateDto);
        model.addAttribute("advertisement", ad);
        return "advertisements/edit";
    }

    /**
     * Обновление рекламы
     */
    @PostMapping("/edit/{id}")
    public String updateAdvertisement(@PathVariable Long id,
                                      @Valid @ModelAttribute("updateDto") UpdateAdvertisementDTO dto,
                                      BindingResult result,
                                      @AuthenticationPrincipal User user,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Optional<AdvertisementDetailDTO> adOptional = advertisementService.getAdvertisementDetails(id, user);
            if (adOptional.isPresent()) {
                model.addAttribute("advertisement", adOptional.get());
            }
            return "advertisements/edit";
        }

        try {
            advertisementService.updateAdvertisement(id, dto, user);
            redirectAttributes.addFlashAttribute("successMessage", "Реклама успешно обновлена");
            return "redirect:/advertisements/details/" + id;

        } catch (Exception e) {
            log.error("Ошибка обновления рекламы: {}", e.getMessage());
            result.reject("advertisement.update.error", "Ошибка обновления: " + e.getMessage());

            Optional<AdvertisementDetailDTO> adOptional = advertisementService.getAdvertisementDetails(id, user);
            if (adOptional.isPresent()) {
                model.addAttribute("advertisement", adOptional.get());
            }
            return "advertisements/edit";
        }
    }

    /**
     * Удаление рекламы
     */
    @PostMapping("/delete/{id}")
    public String deleteAdvertisement(@PathVariable Long id,
                                      @AuthenticationPrincipal User user,
                                      RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = advertisementService.deleteAdvertisement(id, user);
            if (deleted) {
                redirectAttributes.addFlashAttribute("successMessage", "Реклама успешно удалена");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Не удалось удалить рекламу");
            }
        } catch (Exception e) {
            log.error("Ошибка удаления рекламы: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка удаления: " + e.getMessage());
        }

        return "redirect:/advertisements/my";
    }


    // === API ENDPOINTS ===

    /**
     * Получение случайной рекламы для показа
     */
    @GetMapping("/api/random")
    @ResponseBody
    public Optional<AdvertisementResponseDTO> getRandomAd(@AuthenticationPrincipal User user) {
        return advertisementService.getRandomActiveAdvertisement(user);
    }

    /**
     * Регистрация просмотра рекламы
     */
    @PostMapping("/api/{id}/view")
    @ResponseBody
    public String registerView(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            advertisementService.registerView(id, user);
            return "OK";
        } catch (Exception e) {
            log.error("Ошибка регистрации просмотра: {}", e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Регистрация клика по рекламе
     */
    @PostMapping("/api/{id}/click")
    @ResponseBody
    public String registerClick(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            advertisementService.registerClick(id, user);
            return "OK";
        } catch (Exception e) {
            log.error("Ошибка регистрации клика: {}", e.getMessage());
            return "ERROR";
        }
    }
}