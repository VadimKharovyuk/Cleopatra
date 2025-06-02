package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.model.User;
import com.example.cleopatra.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    private final AuthenticationService authenticationService;

    /**
     * Аутентификация пользователя
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletRequest httpRequest) {
        try {
            log.debug("Попытка входа пользователя: {}", request.getEmail());

            Optional<User> userOpt = authenticationService.authenticate(request.getEmail(), request.getPassword());

            if (userOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Неверный email или пароль");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            User user = userOpt.get();
            UserDetails userDetails = authenticationService.loadUserByUsername(user.getEmail());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // Отладка - ДО установки
            log.info("=== ДО установки Authentication ===");
            log.info("Current SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

            // Отладка - ПОСЛЕ установки
            log.info("=== ПОСЛЕ установки Authentication ===");
            log.info("New SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());
            log.info("Session ID: {}", session.getId());
            log.info("Session attribute: {}", session.getAttribute("SPRING_SECURITY_CONTEXT"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Вход выполнен успешно");
            response.put("user", createUserInfo(user));
            response.put("sessionId", session.getId());
            response.put("debugAuth", SecurityContextHolder.getContext().getAuthentication() != null ?
                    SecurityContextHolder.getContext().getAuthentication().getName() : "null");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при входе: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Произошла ошибка при входе");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Выход из системы
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        try {
            // Очищаем SecurityContext
            SecurityContextHolder.clearContext();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Выход выполнен успешно");

            log.info("Пользователь вышел из системы");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при выходе: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Произошла ошибка при выходе");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Проверка статуса аутентификации
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {

            response.put("authenticated", true);
            response.put("username", authentication.getName());
            response.put("authorities", authentication.getAuthorities());

        } else {
            response.put("authenticated", false);
            response.put("username", null);
            response.put("authorities", null);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Проверка валидности сессии
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSession(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Сессия недействительна");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("message", "Сессия действительна");
            response.put("username", authentication.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при валидации сессии: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("message", "Ошибка валидации");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    @GetMapping("/test-auth")
    public ResponseEntity<Map<String, Object>> testAuth(Authentication authentication,
                                                        HttpServletRequest request) {

        log.info("=== TEST-AUTH отладка ===");
        log.info("Authentication parameter: {}", authentication);
        log.info("SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());
        log.info("Session ID: {}", request.getSession().getId());
        log.info("Session attribute: {}", request.getSession().getAttribute("SPRING_SECURITY_CONTEXT"));

        Map<String, Object> response = new HashMap<>();
        response.put("authentication", authentication != null ? authentication.getName() : "null");
        response.put("sessionId", request.getSession().getId());
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        response.put("securityContext", SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "null");

        return ResponseEntity.ok(response);
    }

    /**
     * Создает краткую информацию о пользователе для ответа
     */
    private Map<String, Object> createUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("role", user.getRole().name());
        return userInfo;
    }

    /**
     * DTO для запроса входа
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        private String email;

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 1, message = "Пароль должен содержать минимум 6 символов")
        private String password;
    }
}
