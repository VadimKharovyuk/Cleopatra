package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserStatusController {

    private final UserService userService;

    /**
     * Установить пользователя онлайн
     * Вызывается при загрузке страницы
     */
    @PostMapping("/me/online")
    public ResponseEntity<String> setMeOnline(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);
            userService.setUserOnline(userId, true);

            log.debug("✅ User {} set to ONLINE", userId);
            return ResponseEntity.ok("ONLINE");

        } catch (Exception e) {
            log.error("❌ Error setting user online", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }


    /**
     * Установить пользователя оффлайн
     * Вызывается при закрытии страницы
     */
    @PostMapping("/me/offline")
    public ResponseEntity<String> setMeOffline(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);
            userService.setUserOnline(userId, false);

            log.debug("📴 User {} set to OFFLINE", userId);
            return ResponseEntity.ok("OFFLINE");

        } catch (Exception e) {
            log.error("❌ Error setting user offline", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    /**
     * Ping для поддержания онлайн статуса
     * Вызывается каждые 5 минут
     */
    @PostMapping("/me/ping")
    public ResponseEntity<String> ping(Authentication authentication, HttpServletRequest request) {
//        log.info("=== PING ENDPOINT CALLED ===");
//        log.info("Authentication: {}", authentication);
//        log.info("Is authenticated: {}", authentication != null && authentication.isAuthenticated());
//        log.info("Session ID: {}", request.getSession().getId());
//        log.info("User-Agent: {}", request.getHeader("User-Agent"));
//        log.info("Remote IP: {}", request.getRemoteAddr());

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("❌ Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            log.info("✅ User email: {}", email);

            if ("anonymousUser".equals(email)) {
                log.warn("❌ Anonymous user");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);
//            log.info("✅ User ID: {}", userId);

            userService.setUserOnline(userId, true);
//            log.info("🏓 Ping successful for user {}", userId);

            return ResponseEntity.ok("PING");

        } catch (Exception e) {
            log.error("❌ Error processing ping", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * Получить свой текущий статус (для отладки)
     */
    @GetMapping("/me/status")
    public ResponseEntity<Map<String, Object>> getMyStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Anonymous user"));
            }

            Long userId = userService.getUserIdByEmail(email);
            UserResponse user = userService.getUserById(userId);

            Map<String, Object> response = Map.of(
                    "userId", userId,
                    "email", email,
                    "isOnline", user.getIsOnline(),
                    "lastSeen", user.getLastSeen(),
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting status"));
        }
    }

    /**
     * Получить список пользователей онлайн (опционально)
     */
    @GetMapping("/online")
    public ResponseEntity<List<UserResponse>> getOnlineUsers() {
        try {
            List<UserResponse> onlineUsers = userService.getOnlineUsers();
            return ResponseEntity.ok(onlineUsers);
        } catch (Exception e) {
            log.error("❌ Error getting online users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Проверить статус конкретного пользователя
     */
    @GetMapping("/{userId}/status")
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable Long userId) {
        try {
            boolean isOnline = userService.isUserOnline(userId);
            UserResponse user = userService.getUserById(userId);

            Map<String, Object> response = Map.of(
                    "userId", userId,
                    "isOnline", isOnline,
                    "lastSeen", user.getLastSeen() != null ? user.getLastSeen() : "unknown"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting user status for userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }
    }
}
