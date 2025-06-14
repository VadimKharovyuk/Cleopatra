package com.example.cleopatra.controller;

import com.example.cleopatra.model.User;
import com.example.cleopatra.service.BonusService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/bonus")
@RequiredArgsConstructor
@Slf4j
public class BonusController {

    private final BonusService bonusService;
    private final UserService userService;

    /**
     * Страница приветственного бонуса
     */
    @GetMapping("/welcome")
    public String welcomeBonusPage(Authentication authentication, Model model) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userService.findById(userId);

        BonusService.WelcomeBonusInfo bonusInfo = bonusService.getWelcomeBonusInfo(user);

        model.addAttribute("user", user);
        model.addAttribute("bonusInfo", bonusInfo);
        model.addAttribute("currentBalance", user.getBalance());

        // Если бонус уже получен, перенаправляем на главную
        if (bonusInfo.isAlreadyClaimed()) {
            model.addAttribute("message", "Вы уже получили приветственный бонус!");
            return "redirect:/dashboard"; // или на другую страницу
        }

        return "bonus/welcome";
    }



    /**
     * API для получения информации о бонусе
     */
    @GetMapping("/welcome/info")
    @ResponseBody
    public ResponseEntity<BonusService.WelcomeBonusInfo> getWelcomeBonusInfo(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findById(userId);

        BonusService.WelcomeBonusInfo bonusInfo = bonusService.getWelcomeBonusInfo(user);
        return ResponseEntity.ok(bonusInfo);
    }

    /**
     * API для получения приветственного бонуса
     */
    @PostMapping("/welcome/claim")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> claimWelcomeBonus(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Необходима авторизация"
            ));
        }

        try {
            User user = userService.findById(userId);

            boolean success = bonusService.claimWelcomeBonus(user);

            if (success) {
                // Обновляем данные пользователя после начисления бонуса
                user = userService.findById(userId);

                log.info("Приветственный бонус успешно начислен пользователю ID: {}", userId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Приветственный бонус успешно получен!",
                        "newBalance", user.getBalance(),
                        "bonusAmount", bonusService.getWelcomeBonusInfo(user).getAmount()
                ));
            } else {
                log.warn("Попытка повторного получения приветственного бонуса пользователем ID: {}", userId);

                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Приветственный бонус уже был получен ранее"
                ));
            }

        } catch (Exception e) {
            log.error("Ошибка при получении приветственного бонуса для пользователя ID {}: {}",
                    userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Произошла ошибка при получении бонуса"
            ));
        }
    }

    /**
     * Проверка статуса бонуса для AJAX
     */
    @GetMapping("/welcome/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkBonusStatus(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Необходима авторизация"
            ));
        }

        try {
            User user = userService.findById(userId);

            boolean canClaim = bonusService.canClaimWelcomeBonus(user);

            return ResponseEntity.ok(Map.of(
                    "canClaim", canClaim,
                    "alreadyClaimed", !canClaim,
                    "currentBalance", user.getBalance(),
                    "userId", userId
            ));

        } catch (Exception e) {
            log.error("Ошибка при проверке статуса бонуса для пользователя ID {}: {}",
                    userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Ошибка при проверке статуса бонуса"
            ));
        }
    }

    /**
     * Получение ID текущего пользователя из Authentication
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}