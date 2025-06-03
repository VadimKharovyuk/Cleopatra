package com.example.cleopatra.controller;


import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionListDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.SubscriptionService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionViewController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    /**
     * Страница со списком подписчиков пользователя
     */
    @GetMapping("/{userId}/followers")
    public String showFollowers(@PathVariable Long userId,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                Model model,
                                Authentication authentication) {
        try {
            log.debug("Показать подписчиков пользователя: {}, страница: {}", userId, page);

            // Получаем информацию о пользователе
            UserResponse user = userService.getUserById(userId);
            model.addAttribute("user", user);

            // Получаем подписчиков с пагинацией
            Pageable pageable = PageRequest.of(page, size);
            UserSubscriptionListDto followersDto = subscriptionService.getSubscribers(userId, pageable);
            model.addAttribute("followersDto", followersDto);

            // Текущий пользователь (для проверки подписок)
            if (authentication != null) {
                UserResponse currentUser = userService.getUserByEmail(authentication.getName());
                model.addAttribute("currentUser", currentUser);
            }


            return "subscriptions/followers";

        } catch (Exception e) {
            log.error("Ошибка при показе подписчиков: {}", e.getMessage(), e);
            return "redirect:/error";
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
                                Authentication authentication) {
        try {
            log.debug("Показать подписки пользователя: {}, страница: {}", userId, page);

            // Получаем информацию о пользователе

            UserResponse user = userService.getUserById(userId);
            model.addAttribute("user", user);

            // Получаем подписки с пагинацией
            Pageable pageable = PageRequest.of(page, size);
            UserSubscriptionListDto followingDto = subscriptionService.getSubscriptions(userId, pageable);
            model.addAttribute("followingDto", followingDto);

            // Текущий пользователь (для проверки подписок)
            if (authentication != null) {
                UserResponse currentUser = userService.getUserByEmail(authentication.getName());
                model.addAttribute("currentUser", currentUser);
            }

            return "subscriptions/following";

        } catch (Exception e) {
            log.error("Ошибка при показе подписок: {}", e.getMessage(), e);
            return "redirect:/error";
        }
    }
}
