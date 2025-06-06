package com.example.cleopatra.controller;

import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionListDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.SubscriptionService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionViewController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
//    private final ActivityTracker activityTracker;

    /**
     * Страница со списком подписчиков пользователя
     */
    @GetMapping("/{userId}/followers")
    public String showFollowers(@PathVariable Long userId,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                Model model,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            log.debug("Показать подписчиков пользователя: {}, страница: {}", userId, page);

            // Валидация параметров
            if (userId == null || userId <= 0) {
                log.warn("Некорректный userId: {}", userId);
                redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
                return "redirect:/error";
            }

            if (page < 0) {
                page = 0;
            }

            if (size <= 0 || size > 100) {
                size = 20;
            }

            // Получаем информацию о пользователе
            UserResponse user;
            try {
                user = userService.getUserById(userId);
                if (user == null) {
                    log.warn("Пользователь с ID {} не найден", userId);
                    redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
                    return "redirect:/error";
                }
            } catch (Exception e) {
                log.error("Ошибка при получении пользователя {}: {}", userId, e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
                return "redirect:/error";
            }

            model.addAttribute("user", user);

            // Получаем подписчиков с пагинацией
            Pageable pageable = PageRequest.of(page, size);
            UserSubscriptionListDto followersDto;

            try {
                followersDto = subscriptionService.getSubscribers(userId, pageable);
                if (followersDto == null) {
                    // Создаем пустой DTO если сервис вернул null
                    followersDto = createEmptySubscriptionsList(page, size);
                }
            } catch (Exception e) {
                log.error("Ошибка при получении подписчиков для пользователя {}: {}", userId, e.getMessage());
                followersDto = createEmptySubscriptionsList(page, size);
            }

            model.addAttribute("followersDto", followersDto);

            // Текущий пользователь (для проверки подписок)
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserResponse currentUser = userService.getUserByEmail(authentication.getName());
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("currentUserId", currentUser.getId());
                } catch (Exception e) {
                    log.warn("Не удалось получить текущего пользователя: {}", e.getMessage());

                }
            }

            log.debug("Успешно загружена страница подписчиков для пользователя {}", userId);
            return "subscriptions/followers";

        } catch (Exception e) {
            log.error("Критическая ошибка при показе подписчиков пользователя {}: {}", userId, e.getMessage(), e);

            // Если модель уже частично заполнена, пытаемся показать страницу с ошибкой
            if (!model.containsAttribute("user")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при загрузке страницы");
                return "redirect:/error";
            }
            // ✅ Отслеживаем активность
//            activityTracker.trackActivity(authentication);

            // Если пользователь уже в модели, показываем пустой список
            model.addAttribute("followersDto", createEmptySubscriptionsList(page, size));
            model.addAttribute("errorMessage", "Не удалось загрузить список подписчиков");
            return "subscriptions/followers";
        }
    }

    /**
     * Страница со списком подписок пользователя
     */
    @GetMapping("/{userId}/following")
    public String showFollowing(@PathVariable Long userId,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                Model model,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            log.debug("Показать подписки пользователя: {}, страница: {}", userId, page);

            // Валидация параметров
            if (userId == null || userId <= 0) {
                log.warn("Некорректный userId: {}", userId);
                redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
                return "redirect:/error";
            }

            if (page < 0) {
                page = 0;
            }

            if (size <= 0 || size > 100) {
                size = 20;
            }

            // Получаем информацию о пользователе
            UserResponse user;
            try {
                user = userService.getUserById(userId);
                if (user == null) {
                    log.warn("Пользователь с ID {} не найден", userId);
                    redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
                    return "redirect:/error";
                }
            } catch (Exception e) {
                log.error("Ошибка при получении пользователя {}: {}", userId, e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
                return "redirect:/error";
            }

            model.addAttribute("user", user);

            // Получаем подписки с пагинацией
            Pageable pageable = PageRequest.of(page, size);
            UserSubscriptionListDto followingDto;

            try {
                followingDto = subscriptionService.getSubscriptions(userId, pageable);
                if (followingDto == null) {
                    followingDto = createEmptySubscriptionsList(page, size);
                }
            } catch (Exception e) {
                log.error("Ошибка при получении подписок для пользователя {}: {}", userId, e.getMessage());
                followingDto = createEmptySubscriptionsList(page, size);
            }

            model.addAttribute("followingDto", followingDto);

            // Текущий пользователь (для проверки подписок)
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserResponse currentUser = userService.getUserByEmail(authentication.getName());
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("currentUserId", currentUser.getId());
                } catch (Exception e) {
                    log.warn("Не удалось получить текущего пользователя: {}", e.getMessage());
                }
            }

            log.debug("Успешно загружена страница подписок для пользователя {}", userId);
            return "subscriptions/following";

        } catch (Exception e) {
            log.error("Критическая ошибка при показе подписок пользователя {}: {}", userId, e.getMessage(), e);

            if (!model.containsAttribute("user")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при загрузке страницы");
                return "redirect:/error";
            }

            model.addAttribute("followingDto", createEmptySubscriptionsList(page, size));
            model.addAttribute("errorMessage", "Не удалось загрузить список подписок");
            return "subscriptions/following";
        }
    }

    /**
     * Создает пустой список подписок для случаев ошибки
     */
    private UserSubscriptionListDto createEmptySubscriptionsList(int page, int size) {
        return UserSubscriptionListDto.builder()
                .subscriptions(java.util.Collections.emptyList())
                .currentPage(page)
                .itemsPerPage(size)
                .totalPages(null)
                .totalItems(null)
                .hasNext(false)
                .hasPrevious(page > 0)
                .nextPage(null)
                .previousPage(page > 0 ? page - 1 : null)
                .build();
    }
}