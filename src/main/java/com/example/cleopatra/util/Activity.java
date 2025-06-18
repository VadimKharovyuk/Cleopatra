package com.example.cleopatra.util;

import com.example.cleopatra.service.UserOnlineStatusService;
import com.example.cleopatra.service.UserService;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class Activity {

    public final UserOnlineStatusService userOnlineStatusService;
    private final UserService   userService;


    @GetMapping("/activity")
    public String activity(Model model, Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserIdByEmail(username);

            // АВТОМАТИЧЕСКИ обновляем статус при входе на страницу активности
            log.info("🔄 Пользователь {} зашел на страницу активности, обновляем статус", userId);
            userOnlineStatusService.setUserOnline(userId);

            model.addAttribute("user", userId);
            return "activity";

        } catch (Exception e) {
            log.error("❌ Ошибка при загрузке страницы активности: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка загрузки страницы");
            return "error";
        }
    }
}
