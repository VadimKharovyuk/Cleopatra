package com.example.cleopatra.controller.admin;

import com.example.cleopatra.dto.AdvertisementDTO.AdvertisementList;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.AdvertisementListService;
import com.example.cleopatra.service.AdvertisementService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/advertisements")
@RequiredArgsConstructor
@Slf4j
public class AdvertisementsPanel {

    private final AdvertisementService advertisementService;
    private final AdvertisementListService advertisementListService;
    private final UserService userService;


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String adminPanel(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(required = false) String search,
                             Model model) {
        Pageable pageable = PageRequest.of(page, size);
        AdvertisementList adList;

        if (search != null && !search.trim().isEmpty()) {
            adList = advertisementListService.searchAds(search.trim(), pageable);
            model.addAttribute("search", search);
        } else {
            adList = advertisementListService.getAllAdsForAdmin(pageable);
        }

        model.addAttribute("advertisementList", adList);
        model.addAttribute("currentPage", "admin-ads");
        return "admin/advertisements/admin-list";
    }

    /**
     * Админ панель - рекламы на модерации
     */
    @GetMapping("/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    public String moderationPanel(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  Model model) {
        Pageable pageable = PageRequest.of(page, size);
        AdvertisementList adList = advertisementListService.getAdsForModeration(pageable);

        model.addAttribute("advertisementList", adList);
        model.addAttribute("currentPage", "moderation");
        return "admin/advertisements/admin-moderation";
    }

    /**
     * Админ панель - рекламы с жалобами
     */
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public String reportsPanel(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Model model) {
        Pageable pageable = PageRequest.of(page, size);
        AdvertisementList adList = advertisementListService.getAdsWithReports(pageable);

        model.addAttribute("advertisementList", adList);
        model.addAttribute("currentPage", "reports");
        return "admin/advertisements/admin-reports";
    }


    /**
     * Одобрение рекламы
     */
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveAdvertisement(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getCurrentUserEntity();
            if (admin == null) {
                return "redirect:/login";
            }

            advertisementService.approveAdvertisement(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Реклама успешно одобрена");

        } catch (Exception e) {
            log.error("Ошибка одобрения рекламы {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка одобрения: " + e.getMessage());
        }

        return "redirect:/admin/advertisements/moderation";
    }

    /**
     * Отклонение рекламы
     */
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String rejectAdvertisement(@PathVariable Long id,
                                      @RequestParam String rejectionReason,
                                      @RequestParam(required = false) String rejectionComment,
                                      RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getCurrentUserEntity();
            if (admin == null) {
                return "redirect:/login";
            }

            advertisementService.rejectAdvertisement(id, rejectionReason, rejectionComment, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Реклама отклонена");

        } catch (Exception e) {
            log.error("Ошибка отклонения рекламы {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка отклонения: " + e.getMessage());
        }

        return "redirect:/admin/advertisements/moderation";
    }

    /**
     * Приостановка рекламы
     */
    @PostMapping("/pause/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String pauseAdvertisement(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getCurrentUserEntity();
            if (admin == null) {
                return "redirect:/login";
            }

            advertisementService.pauseAdvertisement(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Реклама приостановлена");

        } catch (Exception e) {
            log.error("Ошибка приостановки рекламы {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка приостановки: " + e.getMessage());
        }

        return "redirect:/admin/advertisements/reports";
    }

    /**
     * Блокировка рекламы
     */
    @PostMapping("/block/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String blockAdvertisement(@PathVariable Long id,
                                     @RequestParam String blockReason,
                                     @RequestParam(required = false) String blockComment,
                                     RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getCurrentUserEntity();
            if (admin == null) {
                return "redirect:/login";
            }

            advertisementService.blockAdvertisement(id, blockReason, blockComment, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Реклама заблокирована");

        } catch (Exception e) {
            log.error("Ошибка блокировки рекламы {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка блокировки: " + e.getMessage());
        }

        return "redirect:/admin/advertisements/reports";
    }

    /**
     * Отклонение жалоб
     */
    @PostMapping("/dismiss-reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String dismissReports(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getCurrentUserEntity();
            if (admin == null) {
                return "redirect:/login";
            }

            advertisementService.dismissReports(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Жалобы отклонены как необоснованные");

        } catch (Exception e) {
            log.error("Ошибка отклонения жалоб для рекламы {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }

        return "redirect:/admin/advertisements/reports";
    }
}