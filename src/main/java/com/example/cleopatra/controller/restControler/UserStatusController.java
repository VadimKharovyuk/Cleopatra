package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.service.UserService;
import com.example.cleopatra.service.UserOnlineStatusService;
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
    private final UserOnlineStatusService userOnlineStatusService;

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

            // ИСПОЛЬЗУЕМ УЛУЧШЕННЫЙ МЕТОД
            userOnlineStatusService.setUserOnline(userId);

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

            // ИСПОЛЬЗУЕМ УЛУЧШЕННЫЙ МЕТОД
            userOnlineStatusService.setUserOffline(userId);

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
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("❌ Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();

            if ("anonymousUser".equals(email)) {
                log.warn("❌ Anonymous user");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);

            // ИСПОЛЬЗУЕМ УЛУЧШЕННЫЙ МЕТОД + ОБНОВЛЯЕМ ВРЕМЯ АКТИВНОСТИ
            userOnlineStatusService.setUserOnline(userId);
            userOnlineStatusService.updateLastSeen(userId);

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

            // ИСПОЛЬЗУЕМ МЕТОДЫ ИЗ UserOnlineStatusService
            boolean isOnline = userOnlineStatusService.isUserOnline(userId);
            java.time.LocalDateTime lastSeen = userOnlineStatusService.getLastSeen(userId);

            UserResponse user = userService.getUserById(userId);

            Map<String, Object> response = Map.of(
                    "userId", userId,
                    "email", email,
                    "isOnline", isOnline,
                    "lastSeen", lastSeen != null ? lastSeen : "unknown",
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
            // ИСПОЛЬЗУЕМ МЕТОДЫ ИЗ UserOnlineStatusService
            boolean isOnline = userOnlineStatusService.isUserOnline(userId);
            java.time.LocalDateTime lastSeen = userOnlineStatusService.getLastSeen(userId);

            Map<String, Object> response = Map.of(
                    "userId", userId,
                    "isOnline", isOnline,
                    "lastSeen", lastSeen != null ? lastSeen : "unknown"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting user status for userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }
    }

    // ====================== ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ДИАГНОСТИКИ ======================

    /**
     * Диагностика статуса текущего пользователя
     */
    @GetMapping("/me/diagnose")
    public ResponseEntity<String> diagnoseMyStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);

            // ВЫЗЫВАЕМ ДИАГНОСТИКУ
            userOnlineStatusService.diagnoseUserStatus(userId);


            return ResponseEntity.ok("✅ Диагностика выполнена для пользователя " + userId + ". Проверьте логи.");

        } catch (Exception e) {
            log.error("❌ Error diagnosing user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * Принудительная пересинхронизация статуса текущего пользователя
     */
    @PostMapping("/me/force-resync")
    public ResponseEntity<String> forceResyncMyStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);

            // ПРИНУДИТЕЛЬНАЯ ПЕРЕСИНХРОНИЗАЦИЯ
            userOnlineStatusService.forceResyncUserStatus(userId, true);

            return ResponseEntity.ok("✅ Принудительная пересинхронизация выполнена для пользователя " + userId);

        } catch (Exception e) {
            log.error("❌ Error force resyncing user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * Пересоздание статуса текущего пользователя
     */
    @PostMapping("/me/recreate")
    public ResponseEntity<String> recreateMyStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);

            // ПОЛНОЕ ПЕРЕСОЗДАНИЕ СТАТУСА
            userOnlineStatusService.recreateUserStatus(userId, true);

            return ResponseEntity.ok("✅ Статус пересоздан для пользователя " + userId);

        } catch (Exception e) {
            log.error("❌ Error recreating user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * Ультра-простое обновление статуса текущего пользователя
     */
    @PostMapping("/me/ultra-simple")
    public ResponseEntity<String> ultraSimpleUpdate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);

            // УЛЬТРА-ПРОСТОЕ ОБНОВЛЕНИЕ
            userOnlineStatusService.updateOnlineStatusUltraSimple(userId, true);

            return ResponseEntity.ok("✅ Ультра-простое обновление выполнено для пользователя " + userId);

        } catch (Exception e) {
            log.error("❌ Error ultra simple update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * Финальное обновление статуса текущего пользователя
     */
    @PostMapping("/me/final-update")
    public ResponseEntity<String> finalUpdate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);

            // ФИНАЛЬНОЕ ОБНОВЛЕНИЕ
            userOnlineStatusService.updateOnlineStatusFinal(userId, true);

            return ResponseEntity.ok("✅ Финальное обновление выполнено для пользователя " + userId);

        } catch (Exception e) {
            log.error("❌ Error final update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * Обновление через новую транзакцию
     */
    @PostMapping("/me/new-transaction")
    public ResponseEntity<String> newTransactionUpdate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        try {
            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Anonymous user");
            }

            Long userId = userService.getUserIdByEmail(email);

            // ОБНОВЛЕНИЕ В НОВОЙ ТРАНЗАКЦИИ
            userOnlineStatusService.updateOnlineStatusNewTransaction(userId, true);

            return ResponseEntity.ok("✅ Обновление в новой транзакции выполнено для пользователя " + userId);

        } catch (Exception e) {
            log.error("❌ Error new transaction update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * Получить количество онлайн пользователей
     */
    @GetMapping("/online/count")
    public ResponseEntity<Map<String, Object>> getOnlineCount() {
        try {
            Long onlineCount = userOnlineStatusService.getOnlineUsersCount();

            Map<String, Object> response = Map.of(
                    "onlineCount", onlineCount,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting online count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting online count"));
        }
    }
}