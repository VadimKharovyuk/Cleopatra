package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.QrAuthSession;
import com.example.cleopatra.model.TrustedDevice;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.TrustedDeviceRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.AuthenticationService;
import com.example.cleopatra.service.QrAuthService;
import com.example.cleopatra.service.TrustedDeviceService;
import com.example.cleopatra.service.UserService;

import com.example.cleopatra.util.DeviceFingerprintUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/auth/qr")
@RequiredArgsConstructor
@Slf4j
public class QrAuthController {

    private final QrAuthService qrAuthService;
    private final TrustedDeviceService trustedDeviceService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;


    /**
     * GET /auth/qr/generate - Генерация QR кода для веб-страницы
     * Возвращает JSON с токеном и ссылкой для QR
     */
    @GetMapping("/generate")
    @ResponseBody
    public ResponseEntity<QrAuthService.QrSessionData> generateQrCode() {
        QrAuthService.QrSessionData sessionData = qrAuthService.generateQrSession();
        return ResponseEntity.ok(sessionData);
    }

    /**
     * GET /auth/qr/{token} - Страница для мобильного устройства
     * Показывает форму входа или автоматически авторизует если устройство доверенное
     */
    @GetMapping("/{token}")
    public String qrLoginPage(@PathVariable String token,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              Model model) {

        // Исправляем тип - используем модель напрямую
        Optional<QrAuthSession> sessionOpt = qrAuthService.getActiveSession(token);
        if (sessionOpt.isEmpty()) {
            model.addAttribute("error", "QR-код истек или недействителен");
            return "qr-auth/expired";
        }

        // Получаем deviceId из cookie
        String deviceId = getDeviceIdFromCookie(request);

        // Если deviceId есть, проверяем среди всех пользователей
        if (deviceId != null) {
            // Здесь нужно найти пользователя по доверенному устройству
            Optional<User> userOpt = trustedDeviceService.findUserByTrustedDevice(deviceId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Устройство доверенное - автоматически подтверждаем
                boolean confirmed = qrAuthService.confirmQrAuth(token, user.getId());

                if (confirmed) {
                    trustedDeviceService.updateLastUsed(deviceId);
                    model.addAttribute("success", true);
                    model.addAttribute("userName", user.getEmail());
                    return "qr-auth/auto-login";
                }
            }
        }

        // Устройство не доверенное - показываем форму входа
        model.addAttribute("token", token);
        return "auth/login";
    }

    /**
     * POST /auth/qr/confirm - Подтверждение входа с мобильного устройства
     */
    @PostMapping("/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmQrLogin(@RequestBody QrLoginRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {

        log.info("QR confirm request received for token: {} and email: {}", request.getToken(), request.getEmail());

        // Проверяем учетные данные
        Optional<User> userOpt = authenticationService.authenticate(request.getEmail(), request.getPassword());

        if (userOpt.isEmpty()) {
            log.warn("Authentication failed for email: {}", request.getEmail());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Неверный email или пароль"));
        }

        User user = userOpt.get();
        log.info("User authenticated successfully: {} (ID: {})", user.getEmail(), user.getId());

        // Подтверждаем QR-авторизацию
        boolean confirmed = qrAuthService.confirmQrAuth(request.getToken(), user.getId());
        log.info("QR confirmation result: {}", confirmed);

        if (!confirmed) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "QR-код истек или недействителен"));
        }

        // Остальной код...
        return ResponseEntity.ok(Map.of("success", true, "message", "Вход выполнен успешно"));
    }

    /**
     * GET /auth/qr/status/{token} - Проверка статуса QR-сессии (для polling)
     */
    @GetMapping("/status/{token}")
    @ResponseBody
    public ResponseEntity<QrAuthService.QrStatusData> checkQrStatus(@PathVariable String token) {
        QrAuthService.QrStatusData statusData = qrAuthService.checkQrStatus(token);
        return ResponseEntity.ok(statusData);
    }


    /**
     * GET /auth/qr/complete/{token} - Завершение QR-авторизации на веб-клиенте
     */
    @GetMapping("/complete/{token}")
    public String completeQrAuth(@PathVariable String token,
                                 HttpServletRequest request) {

        log.info("Completing QR auth for token: {}", token);

        // Проверяем статус QR-сессии
        QrAuthService.QrStatusData statusData = qrAuthService.checkQrStatus(token);

        if (!statusData.isConfirmed() || statusData.getUserId() == null) {
            log.warn("QR session not confirmed or no user: {}", statusData.getStatus());
            return "redirect:/login?error=qr_not_confirmed";
        }

        try {
            // Получаем пользователя
            Optional<User> userOpt = userRepository.findById(statusData.getUserId());
            if (userOpt.isEmpty()) {
                log.error("User not found: {}", statusData.getUserId());
                return "redirect:/login?error=user_not_found";
            }

            User user = userOpt.get();

            // Создаем UserDetails для Spring Security
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities("ROLE_" + user.getRole().name())
                    .build();

            // Создаем Authentication объект
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // Устанавливаем в Security Context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Сохраняем сессию
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            log.info("QR authentication completed successfully for user: {}", user.getEmail());

            return "redirect:/dashboard"; // Или ваша главная страница

        } catch (Exception e) {
            log.error("Error completing QR authentication: {}", e.getMessage(), e);
            return "redirect:/login?error=qr_auth_failed";
        }
    }
    // Вспомогательные методы

    private String getDeviceIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("deviceId".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setDeviceIdCookie(HttpServletResponse response, String deviceId) {
        Cookie cookie = new Cookie("deviceId", deviceId);
        cookie.setMaxAge(60 * 60 * 24 * 365); // 1 год
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // для localhost, в продакшене должно быть true
        cookie.setPath("/");
        response.addCookie(cookie);
    }


    // DTO для запроса подтверждения
    @Getter
    public static class QrLoginRequest {
        // Getters и Setters
        private String token;
        private String email;
        private String password;
        private boolean trustDevice;

        public void setToken(String token) {
            this.token = token;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setTrustDevice(boolean trustDevice) {
            this.trustDevice = trustDevice;
        }
    }
}


