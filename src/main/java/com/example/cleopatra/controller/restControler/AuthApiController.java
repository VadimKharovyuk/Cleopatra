package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final UserRepository userRepository;

    /**
     * Аутентификация пользователя
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletRequest httpRequest) {

        try {
            // Шаг 1: Проверяем входные данные
            log.info("🔍 ШАГ 1: Проверка входных данных");
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.error("❌ Email пустой или null");
                return createErrorResponse("Email не может быть пустым", HttpStatus.BAD_REQUEST);
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                log.error("❌ Пароль пустой или null");
                return createErrorResponse("Пароль не может быть пустым", HttpStatus.BAD_REQUEST);
            }

            // ДОБАВЛЯЕМ: Проверяем, не заблокирован ли пользователь ДО аутентификации
            try {
                Optional<User> blockedUserOpt = userRepository.findByEmail(request.getEmail());
                if (blockedUserOpt.isPresent() && Boolean.TRUE.equals(blockedUserOpt.get().getIsBlocked())) {
                    User blockedUser = blockedUserOpt.get();
                    log.warn("🚫 User {} is blocked, sending blocked response", request.getEmail());

                    Map<String, Object> blockedResponse = new HashMap<>();
                    blockedResponse.put("success", false);
                    blockedResponse.put("blocked", true);
                    blockedResponse.put("message", "Ваш аккаунт заблокирован. Обратитесь в поддержку.");
                    blockedResponse.put("redirectUrl", "/blocked-account");

                    // Добавляем дополнительную информацию о блокировке
                    blockedResponse.put("blockInfo", Map.of(
                            "blockedAt", blockedUser.getBlockedAt() != null ? blockedUser.getBlockedAt().toString() : null,
                            "reason", blockedUser.getBlockReason() != null ? blockedUser.getBlockReason() : "Причина не указана",
                            "supportEmail", "support@cleopatra.com"
                    ));

                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(blockedResponse);
                }
            } catch (Exception e) {
                log.error("❌ Ошибка при проверке блокировки: {}", e.getMessage());
                // Продолжаем выполнение, если проверка блокировки не удалась
            }

            log.info("✅ Входные данные валидны");

            // Шаг 2: Аутентификация
            Optional<User> userOpt;

            try {
                userOpt = authenticationService.authenticate(request.getEmail(), request.getPassword());
                log.info("✅ authenticationService.authenticate() выполнен успешно");
            } catch (Exception e) {
                log.error("❌ ОШИБКА в authenticationService.authenticate(): {}", e.getMessage(), e);
                log.error("❌ Exception class: {}", e.getClass().getName());
                return createErrorResponse("Ошибка аутентификации: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Шаг 3: Проверяем результат аутентификации
            if (userOpt.isEmpty()) {
                log.warn("🔒 Аутентификация не прошла для email: {}", request.getEmail());

                // Дополнительная проверка - может быть пользователь заблокирован
                try {
                    Optional<User> userCheckOpt = userRepository.findByEmail(request.getEmail());
                    if (userCheckOpt.isPresent() && Boolean.TRUE.equals(userCheckOpt.get().getIsBlocked())) {
                        log.warn("🚫 User {} is blocked (detected in step 3)", request.getEmail());

                        Map<String, Object> blockedResponse = new HashMap<>();
                        blockedResponse.put("success", false);
                        blockedResponse.put("blocked", true);
                        blockedResponse.put("message", "Ваш аккаунт заблокирован. Обратитесь в поддержку.");
                        blockedResponse.put("redirectUrl", "/blocked-account");

                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(blockedResponse);
                    }
                } catch (Exception e) {
                    log.error("❌ Ошибка при дополнительной проверке блокировки: {}", e.getMessage());
                }

                return createErrorResponse("Неверный email или пароль", HttpStatus.UNAUTHORIZED);
            }

            User user = userOpt.get();

            // Шаг 4: Загрузка UserDetails
            UserDetails userDetails;

            try {
                userDetails = authenticationService.loadUserByUsername(user.getEmail());
                log.info("✅ UserDetails загружены успешно: {}", userDetails.getUsername());
            } catch (UsernameNotFoundException e) {
                // Проверяем, не связано ли это с блокировкой
                if (e.getMessage().contains("blocked")) {
                    log.warn("🚫 User {} blocked during UserDetails loading", user.getEmail());

                    Map<String, Object> blockedResponse = new HashMap<>();
                    blockedResponse.put("success", false);
                    blockedResponse.put("blocked", true);
                    blockedResponse.put("message", "Ваш аккаунт был заблокирован.");
                    blockedResponse.put("redirectUrl", "/blocked-account");

                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(blockedResponse);
                }

                log.error("❌ ОШИБКА в loadUserByUsername(): {}", e.getMessage(), e);
                return createErrorResponse("Пользователь не найден: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("❌ ОШИБКА в loadUserByUsername(): {}", e.getMessage(), e);
                log.error("❌ Exception class: {}", e.getClass().getName());
                return createErrorResponse("Ошибка загрузки данных пользователя: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Шаг 5: Создание Authentication
            Authentication authentication;

            try {
                authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
            } catch (Exception e) {
                log.error("❌ ОШИБКА при создании Authentication: {}", e.getMessage(), e);
                return createErrorResponse("Ошибка создания токена аутентификации: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Шаг 6: Установка в SecurityContext
            try {
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
            } catch (Exception e) {
                log.error("❌ ОШИБКА при установке SecurityContext: {}", e.getMessage(), e);
                return createErrorResponse("Ошибка установки контекста безопасности: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Шаг 7: Работа с сессией
            HttpSession session;

            try {
                session = httpRequest.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            } catch (Exception e) {
                log.error("❌ ОШИБКА при работе с сессией: {}", e.getMessage(), e);
                return createErrorResponse("Ошибка создания сессии: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Шаг 8: Создание ответа
            Map<String, Object> userInfo;

            try {
                userInfo = createUserInfo(user);
            } catch (Exception e) {
                log.error("❌ ОШИБКА при создании UserInfo: {}", e.getMessage(), e);
                return createErrorResponse("Ошибка формирования данных пользователя: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Вход выполнен успешно");
            response.put("user", userInfo);
            response.put("sessionId", session.getId());
            response.put("debugAuth", SecurityContextHolder.getContext().getAuthentication() != null ?
                    SecurityContextHolder.getContext().getAuthentication().getName() : "null");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ === КРИТИЧЕСКАЯ ОШИБКА В ЛОГИНЕ ===");
            log.error("❌ Message: {}", e.getMessage());
            log.error("❌ Class: {}", e.getClass().getName());
            log.error("❌ Stack trace: ", e);

            return createErrorResponse("Произошла неожиданная ошибка при входе: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Вспомогательные методы
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.status(status).body(errorResponse);
    }

    private Map<String, Object> createUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("role", user.getRole().name());
        userInfo.put("imageUrl", user.getImageUrl());
        userInfo.put("isPrivateProfile", user.getIsPrivateProfile());
        userInfo.put("followersCount", user.getFollowersCount());
        userInfo.put("followingCount", user.getFollowingCount());

        return userInfo;
    }
//    @PostMapping("/login")
//    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
//                                                     HttpServletRequest httpRequest) {
//        try {
//            log.debug("Попытка входа пользователя: {}", request.getEmail());
//
//            Optional<User> userOpt = authenticationService.authenticate(request.getEmail(), request.getPassword());
//
//            if (userOpt.isEmpty()) {
//                Map<String, Object> errorResponse = new HashMap<>();
//                errorResponse.put("success", false);
//                errorResponse.put("message", "Неверный email или пароль");
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
//            }
//
//            User user = userOpt.get();
//            UserDetails userDetails = authenticationService.loadUserByUsername(user.getEmail());
//            Authentication authentication = new UsernamePasswordAuthenticationToken(
//                    userDetails, null, userDetails.getAuthorities());
//
//            // Отладка - ДО установки
//            log.info("=== ДО установки Authentication ===");
//            log.info("Current SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());
//
//            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
//            securityContext.setAuthentication(authentication);
//            SecurityContextHolder.setContext(securityContext);
//
//            HttpSession session = httpRequest.getSession(true);
//            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
//
//            // Отладка - ПОСЛЕ установки
//            log.info("=== ПОСЛЕ установки Authentication ===");
//            log.info("New SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());
//            log.info("Session ID: {}", session.getId());
//            log.info("Session attribute: {}", session.getAttribute("SPRING_SECURITY_CONTEXT"));
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", "Вход выполнен успешно");
//            response.put("user", createUserInfo(user));
//            response.put("sessionId", session.getId());
//            response.put("debugAuth", SecurityContextHolder.getContext().getAuthentication() != null ?
//                    SecurityContextHolder.getContext().getAuthentication().getName() : "null");
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("Ошибка при входе: {}", e.getMessage(), e);
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("success", false);
//            errorResponse.put("message", "Произошла ошибка при входе");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//        }
//    }

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

    // Добавьте тестовый endpoint
    @GetMapping("/test-db")
    public ResponseEntity<?> testDb() {
        try {
            long count = userRepository.count();
            return ResponseEntity.ok(Map.of("userCount", count));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
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
