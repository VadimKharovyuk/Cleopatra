package com.example.cleopatra.controller;

import com.example.cleopatra.dto.AdvertisementDTO.*;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.AdCategory;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.model.Advertisement;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.AdvertisementRepository;
import com.example.cleopatra.service.AdvertisementListService;
import com.example.cleopatra.service.AdvertisementService;
import com.example.cleopatra.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/advertisements")
@RequiredArgsConstructor
@Slf4j
public class AdvertisementController {

    private final AdvertisementService advertisementService;
    private final AdvertisementListService advertisementListService;
    private final UserService userService;
    private final AdvertisementRepository advertisementRepository;


    @GetMapping("/info")
    public String info(Model model,Authentication authentication) {
        String email = authentication.getName();
        Long userId = userService.getUserIdByEmail(email);
        UserResponse currentUser = userService.getUserById(userId);

        model.addAttribute("currentUserId", currentUser);
        return "advertisements/info";
    }

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
    public String createForm(Authentication authentication, Model model) {
        // Получаем текущего пользователя
        User user = userService.getCurrentUserEntity(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("createDto", new CreateAdvertisementDTO());
        model.addAttribute("categories", AdCategory.values());
        model.addAttribute("genders", Gender.values());

        // Передаем баланс как строку и как BigDecimal
        BigDecimal userBalance = user.getBalance();
        model.addAttribute("userBalance", userBalance);
        model.addAttribute("userBalanceString", userBalance.toString());

        // Для отладки
        log.info("Баланс пользователя {}: {}", user.getEmail(), userBalance);

        return "advertisements/create";
    }

    @PostMapping("/create")
    public String createAdvertisement(@Valid @ModelAttribute("createDto") CreateAdvertisementDTO dto,
                                      BindingResult result,
                                      @RequestParam(value = "image", required = false) MultipartFile image,
                                      Authentication authentication,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {

        // Получаем пользователя
        User user = userService.getCurrentUserEntity(authentication);
        if (user == null) {
            log.error("Пользователь не найден");
            redirectAttributes.addFlashAttribute("errorMessage", "Необходимо войти в систему");
            return "redirect:/login";
        }

        log.debug("Создание рекламы для пользователя: {}", user.getEmail());

        // НОВАЯ ПРОВЕРКА: Достаточно ли средств на балансе
        if (dto.getTotalBudget() != null && user.getBalance().compareTo(dto.getTotalBudget()) < 0) {
            result.rejectValue("totalBudget", "insufficient.balance",
                    "Недостаточно средств на балансе. Ваш баланс: " + user.getBalance() + " руб.");
        }

        if (result.hasErrors()) {
            model.addAttribute("categories", AdCategory.values());
            model.addAttribute("genders", Gender.values());
            model.addAttribute("userBalance", user.getBalance()); // Добавляем баланс обратно
            return "advertisements/create";
        }

        try {
            // НОВАЯ ЛОГИКА: Резервируем средства при создании рекламы
            if (dto.getTotalBudget() != null && dto.getTotalBudget().compareTo(BigDecimal.ZERO) > 0) {
                // Проверяем еще раз перед списанием
                if (user.getBalance().compareTo(dto.getTotalBudget()) < 0) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Недостаточно средств на балансе для создания рекламы");
                    return "redirect:/advertisements/create";
                }

                // Резервируем средства (списываем с баланса пользователя)
                userService.subtractBalance(user, dto.getTotalBudget());
                log.info("Зарезервировано {} руб. для рекламы пользователя {}",
                        dto.getTotalBudget(), user.getEmail());
            }

            AdvertisementDetailDTO createdAd;
            if (image != null && !image.isEmpty()) {
                createdAd = advertisementService.createAdvertisement(dto, image, user);
            } else {
                createdAd = advertisementService.createAdvertisement(dto, user);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Реклама успешно создана! Ожидает модерации. Зарезервировано " +
                            dto.getTotalBudget() + " руб. из вашего баланса.");
            return "redirect:/advertisements/details/" + createdAd.getId();

        } catch (IllegalArgumentException e) {
            log.error("Ошибка баланса при создании рекламы: {}", e.getMessage());
            result.reject("balance.error", e.getMessage());
        } catch (IOException e) {
            log.error("Ошибка загрузки изображения: {}", e.getMessage());
            result.reject("image.upload.error", "Ошибка загрузки изображения: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка создания рекламы: {}", e.getMessage());
            result.reject("advertisement.create.error", "Ошибка создания рекламы: " + e.getMessage());

            // ВАЖНО: Если произошла ошибка после списания, возвращаем средства
            if (dto.getTotalBudget() != null && dto.getTotalBudget().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    userService.addBalance(user, dto.getTotalBudget());
                    log.info("Возвращены средства {} руб. пользователю {} из-за ошибки создания рекламы",
                            dto.getTotalBudget(), user.getEmail());
                } catch (Exception balanceError) {
                    log.error("Критическая ошибка возврата средств: {}", balanceError.getMessage());
                }
            }
        }

        model.addAttribute("categories", AdCategory.values());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("userBalance", user.getBalance()); // Обновленный баланс
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
    @GetMapping("/api/random")
    @ResponseBody
    public ResponseEntity<?> getRandomAd(HttpServletRequest request) {
        try {
            List<Advertisement> activeAds = advertisementRepository.findByStatus(AdStatus.ACTIVE);

            if (activeAds.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            Advertisement ad = activeAds.get(0);

            Map<String, Object> result = new HashMap<>();
            result.put("id", ad.getId());
            result.put("title", ad.getTitle() != null ? ad.getTitle() : "");
            result.put("shortDescription", ad.getShortDescription() != null ? ad.getShortDescription() : "");
            result.put("url", ad.getUrl() != null ? ad.getUrl() : "");
            result.put("imageUrl", ad.getImageUrl() != null ? ad.getImageUrl() : "");
            result.put("category", ad.getCategory() != null ? ad.getCategory().name() : "");
            result.put("viewsCount", ad.getViewsCount() != null ? ad.getViewsCount() : 0);
            result.put("clicksCount", ad.getClicksCount() != null ? ad.getClicksCount() : 0);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Ошибка загрузки рекламы",
                            "details", e.getMessage()
                    ));
        }
    }

    /**
     * Регистрация просмотра рекламы
     */
    @PostMapping("/api/{id}/view")
    @ResponseBody
    public ResponseEntity<String> registerView(@PathVariable Long id,
                                               Authentication authentication,
                                               HttpServletRequest request) {
        try {
            User user = userService.getCurrentUserEntity(authentication);
            log.info("Регистрация просмотра рекламы {} пользователем {}",
                    id, user != null ? user.getEmail() : "анонимный");

            // Передаем request в сервис
            advertisementService.registerView(id, user, request);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Ошибка регистрации просмотра: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("ERROR");
        }
    }

    @PostMapping("/api/{id}/click")
    @ResponseBody
    public ResponseEntity<String> registerClick(@PathVariable Long id,
                                                Authentication authentication,
                                                HttpServletRequest request) {
        try {
            User user = userService.getCurrentUserEntity(authentication);
            log.info("Регистрация клика по рекламе {} пользователем {}",
                    id, user != null ? user.getEmail() : "анонимный");

            // Передаем request в сервис
            advertisementService.registerClick(id, user, request);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Ошибка регистрации клика: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("ERROR");
        }
    }
}