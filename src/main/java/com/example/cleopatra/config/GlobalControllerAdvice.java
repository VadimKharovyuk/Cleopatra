package com.example.cleopatra.config;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.MessageService;
import com.example.cleopatra.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Глобальный ControllerAdvice для автоматического добавления
 * текущего пользователя во все модели контроллеров
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalControllerAdvice {

    private final UserService userService;
    private final MessageService messageService;

    /**
     * Автоматически добавляет текущего пользователя в модель для всех контроллеров
     *
     * @return UserResponse текущего пользователя или null если не авторизован
     */
    @ModelAttribute("user")
    public UserResponse getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Проверяем, что пользователь авторизован и не анонимный
            if (authentication == null ||
                    !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                log.debug("👤 Пользователь не авторизован");
                return null;
            }

            String userEmail = authentication.getName();
//            log.debug("🔍 Получаем пользователя для email: {}", userEmail);

            UserResponse currentUser = userService.getUserByEmail(userEmail);
//            log.debug("✅ Пользователь найден: {} (ID: {})",
//                    currentUser.getFirstName() != null ? currentUser.getFirstName() : "Unnamed",
//                    currentUser.getId());

            return currentUser;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении текущего пользователя: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Добавляет информацию о том, авторизован ли пользователь
     *
     * @return true если пользователь авторизован
     */
    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Добавляет ID текущего пользователя для удобства
     *
     * @return ID пользователя или null
     */
    @ModelAttribute("currentUserId")
    public Long getCurrentUserId() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();

            String userAgent = request.getHeader("User-Agent");
            String requestURI = request.getRequestURI();
            String method = request.getMethod();


            log.debug("🔍 getCurrentUserId() - URI: {}, Method: {}, UserAgent: {}",
                    requestURI, method, userAgent);

            UserResponse user = getCurrentUser();
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            log.debug("❌ Ошибка в getCurrentUserId(): {}", e.getMessage());
            return null;
        }
    }
//    @ModelAttribute("currentUserId")
//    public Long getCurrentUserId() {
//        UserResponse user = getCurrentUser();
//        return user != null ? user.getId() : null;
//    }

    /**
     * Добавляет email текущего пользователя
     *
     * @return email пользователя или null
     */
    @ModelAttribute("currentUserEmail")
    public String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Не удалось получить email пользователя: {}", e.getMessage());
        }
        return null;
    }


    /**
     * Возвращает роль текущего пользователя
     */
    @ModelAttribute("userRole")
    public String getUserRole() {
        UserResponse user = getCurrentUser();
        return user != null && user.getRole() != null ? user.getRole().name() : null;
    }

    /**
     * Проверяет, является ли пользователь администратором
     */
    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        UserResponse user = getCurrentUser();
        return user != null && user.getRole() != null &&
                "ADMIN".equals(user.getRole().name());
    }

    @ModelAttribute("activeSection")
    public String getActiveSection(HttpServletRequest request) {
        String path = request.getServletPath();

        if (path.startsWith("/dashboard")) return "dashboard";
        if (path.startsWith("/profile")) return "profile";
        if (path.startsWith("/messages")) return "messages";
        if (path.startsWith("/notifications")) return "notifications";
        if (path.startsWith("/bookmarks")) return "bookmarks";
        if (path.startsWith("/settings")) return "settings";
        if (path.startsWith("/recommendations")) return "recommendations";

        return "dashboard";
    }

    @ModelAttribute()
    public String totalUnread(Model model) {

        Long totalUnread = messageService.getUnreadMessagesCount();
        model.addAttribute("totalUnread", totalUnread.intValue());
        return null;
    }

}