package com.example.cleopatra.util;

import com.example.cleopatra.dto.AdvertisementDTO.AdvertisementResponseDTO;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.model.Advertisement;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.AdvertisementRepository;
import com.example.cleopatra.service.AdvertisementService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final AdvertisementService advertisementService;
    private final UserService userService;
    private final AdvertisementRepository advertisementRepository ;

    @GetMapping
    public String test(Authentication authentication, Model model) {
        log.info("=== ТЕСТИРОВАНИЕ РЕКЛАМЫ ===");

        try {
            // Получаем текущего пользователя
            User user = null;
            if (authentication != null) {
                try {
                    user = userService.getCurrentUserEntity(authentication);
                    log.info("Пользователь найден: {} (пол: {})",
                            user.getEmail(),
                            user.getGender() != null ? user.getGender().name() : "не указан");
                } catch (Exception e) {
                    log.warn("Ошибка получения пользователя: {}", e.getMessage());
                }
            } else {
                log.info("Пользователь не авторизован");
            }

            // Получаем случайную рекламу
            log.info("Пытаемся получить случайную рекламу...");
            Optional<AdvertisementResponseDTO> randomAd = advertisementService.getRandomActiveAdvertisement(user);

            // Подробная диагностика
            if (randomAd.isPresent()) {
                AdvertisementResponseDTO ad = randomAd.get();
                log.info("✅ РЕКЛАМА НАЙДЕНА:");
                log.info("  ID: {}", ad.getId());
                log.info("  Заголовок: {}", ad.getTitle());
                log.info("  Описание: {}", ad.getShortDescription());
                log.info("  URL: {}", ad.getUrl());
                log.info("  Изображение: {}", ad.getImageUrl());
                log.info("  Категория: {}", ad.getCategory());

                model.addAttribute("advertisement", ad);
                model.addAttribute("hasAdvertisement", true);

            } else {
                log.warn("❌ РЕКЛАМА НЕ НАЙДЕНА");
                model.addAttribute("hasAdvertisement", false);

                // Дополнительная диагностика
                checkAdvertisementsInDatabase();
            }

        } catch (Exception e) {
            log.error("❌ ОШИБКА при получении рекламы: {}", e.getMessage(), e);
            model.addAttribute("hasAdvertisement", false);
            model.addAttribute("error", "Ошибка загрузки рекламы: " + e.getMessage());
        }

        log.info("=== РЕЗУЛЬТАТ: hasAdvertisement = {} ===", model.getAttribute("hasAdvertisement"));
        return "test";
    }

    // Добавим отладочный метод для проверки базы данных
    private void checkAdvertisementsInDatabase() {
        try {
            // Этот метод нужно будет добавить в сервис
            log.info("Проверяем базу данных...");
            // Здесь можно добавить прямую проверку через Repository
        } catch (Exception e) {
            log.error("Ошибка проверки базы: {}", e.getMessage());
        }
    }

    // Добавьте эти методы в TestController для диагностики:

    @GetMapping("/debug")
    @ResponseBody
    public Map<String, Object> debugAdvertisements(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Проверяем пользователя
            User user = null;
            if (authentication != null) {
                try {
                    user = userService.getCurrentUserEntity(authentication);
                    result.put("user_found", true);
                    result.put("user_email", user.getEmail());
                    result.put("user_gender", user.getGender() != null ? user.getGender().name() : null);
                } catch (Exception e) {
                    result.put("user_error", e.getMessage());
                }
            } else {
                result.put("user_found", false);
            }

            // 2. Проверяем рекламу через сервис
            try {
                Optional<AdvertisementResponseDTO> randomAd = advertisementService.getRandomActiveAdvertisement(user);
                result.put("service_ad_found", randomAd.isPresent());
                if (randomAd.isPresent()) {
                    result.put("service_ad", randomAd.get());
                }
            } catch (Exception e) {
                result.put("service_error", e.getMessage());
            }

            // 3. Проверяем через прямой вызов Repository (добавьте этот метод в сервис)
            try {
                result.put("direct_check", "Нужно добавить метод проверки в сервис");
            } catch (Exception e) {
                result.put("direct_error", e.getMessage());
            }

        } catch (Exception e) {
            result.put("general_error", e.getMessage());
        }

        return result;
    }

    // Добавьте этот ВРЕМЕННЫЙ endpoint в AdvertisementController для отладки:

    @GetMapping("/api/random-debug")
    @ResponseBody
    public ResponseEntity<?> getRandomAdDebug() {
        try {
            log.info("=== ОТЛАДОЧНЫЙ API ENDPOINT ===");

            // Проверяем, работает ли Repository
            log.info("Шаг 1: Проверяем Repository...");
            long totalCount = advertisementRepository.count();
            log.info("Всего объявлений в базе: {}", totalCount);

            if (totalCount == 0) {
                return ResponseEntity.ok(Map.of("error", "Нет объявлений в базе"));
            }

            // Пробуем получить активные объявления
            log.info("Шаг 2: Получаем активные объявления...");
            List<Advertisement> activeAds = advertisementRepository.findByStatus(AdStatus.ACTIVE);
            log.info("Найдено активных: {}", activeAds.size());

            if (activeAds.isEmpty()) {
                // Пробуем получить любые объявления
                List<Advertisement> allAds = advertisementRepository.findAll();
                log.info("Всего объявлений (любых): {}", allAds.size());

                return ResponseEntity.ok(Map.of(
                        "error", "Нет активных объявлений",
                        "total_ads", allAds.size(),
                        "active_ads", 0
                ));
            }

            // Выбираем первое активное объявление
            log.info("Шаг 3: Преобразуем в DTO...");
            Advertisement firstAd = activeAds.get(0);

            try {
                AdvertisementResponseDTO dto = AdvertisementResponseDTO.fromEntity(firstAd);
                log.info("DTO создан успешно: {}", dto.getTitle());

                return ResponseEntity.ok(dto);

            } catch (Exception e) {
                log.error("Ошибка создания DTO: {}", e.getMessage(), e);

                // Возвращаем базовую информацию вручную
                Map<String, Object> manualDto = new HashMap<>();
                manualDto.put("id", firstAd.getId());
                manualDto.put("title", firstAd.getTitle());
                manualDto.put("shortDescription", firstAd.getShortDescription());
                manualDto.put("url", firstAd.getUrl());
                manualDto.put("imageUrl", firstAd.getImageUrl());
                manualDto.put("category", firstAd.getCategory() != null ? firstAd.getCategory().name() : null);
                manualDto.put("viewsCount", firstAd.getViewsCount() != null ? firstAd.getViewsCount() : 0);
                manualDto.put("clicksCount", firstAd.getClicksCount() != null ? firstAd.getClicksCount() : 0);

                return ResponseEntity.ok(manualDto);
            }

        } catch (Exception e) {
            log.error("Критическая ошибка в отладочном API: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Критическая ошибка",
                            "message", e.getMessage(),
                            "type", e.getClass().getSimpleName()
                    ));
        }
    }

    // Также добавьте простой endpoint для проверки работоспособности:
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<?> healthCheck() {
        try {
            long count = advertisementRepository.count();
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "total_advertisements", count,
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}