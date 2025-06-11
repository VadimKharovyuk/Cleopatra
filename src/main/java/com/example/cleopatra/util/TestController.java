package com.example.cleopatra.util;

import com.example.cleopatra.dto.AdvertisementDTO.AdvertisementResponseDTO;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.AdvertisementService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final AdvertisementService advertisementService;
    private final UserService userService;

    @GetMapping
    public String test(Authentication authentication, Model model) {
        try {
            // Получаем текущего пользователя
            User user = null;
            if (authentication != null) {
                user = userService.getCurrentUserEntity(authentication);
            }

            // Получаем случайную рекламу
            Optional<AdvertisementResponseDTO> randomAd = advertisementService.getRandomActiveAdvertisement(user);

            // Добавляем в модель
            if (randomAd.isPresent()) {
                model.addAttribute("advertisement", randomAd.get());
                model.addAttribute("hasAdvertisement", true);
                log.debug("Добавлена реклама в модель: {}", randomAd.get().getTitle());
            } else {
                model.addAttribute("hasAdvertisement", false);
                log.debug("Активная реклама не найдена");
            }

        } catch (Exception e) {
            log.error("Ошибка при получении рекламы для тестовой страницы: {}", e.getMessage());
            model.addAttribute("hasAdvertisement", false);
            model.addAttribute("error", "Ошибка загрузки рекламы: " + e.getMessage());
        }

        return "test";
    }
}