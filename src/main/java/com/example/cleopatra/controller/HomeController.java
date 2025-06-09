package com.example.cleopatra.controller;

import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
@Slf4j
@RequiredArgsConstructor
@Controller
public class HomeController {
    private final UserService userService;


    @GetMapping
    public String home(@ModelAttribute("currentUserId") Long currentUserId, Model model) {

        try {
            // Если пользователь авторизован
            if (currentUserId != null) {
                log.info("Авторизованный пользователь {} заходит на главную страницу", currentUserId);

                // Проверяем существование пользователя
                if (userService.userExists(currentUserId)) {
                    log.info("Авторизованный пользователь {} просматривает главную страницу", currentUserId);

                    // Передаем дополнительную информацию для авторизованного пользователя
                    model.addAttribute("isAuthenticated", true);
                    model.addAttribute("userProfileUrl", "/profile/" + currentUserId);

                    return "home"; // Показываем главную страницу
                } else {
                    // Пользователь в сессии, но не существует в БД (был удален)
                    log.warn("Пользователь {} в сессии, но не найден в БД. Очищаем сессию", currentUserId);
                    return "redirect:/logout"; // Принудительный logout
                }
            }

            // Пользователь не авторизован - показываем обычную главную страницу
            log.info("Неавторизованный пользователь заходит на главную страницу");
            model.addAttribute("isAuthenticated", false);
            return "home";

        } catch (Exception e) {
            log.error("Ошибка при обработке главной страницы для пользователя {}: {}",
                    currentUserId, e.getMessage(), e);

            // В случае ошибки показываем главную страницу
            model.addAttribute("isAuthenticated", false);
//            return "homeV1";
            return "home";
        }
    }
}
