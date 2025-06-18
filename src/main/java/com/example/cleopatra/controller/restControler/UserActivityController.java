package com.example.cleopatra.controller.restControler;// ====================== ДОБАВЬТЕ В ВАШ КОНТРОЛЛЕР ======================

import com.example.cleopatra.service.UserOnlineStatusService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserActivityController {

    private final UserOnlineStatusService userOnlineStatusService;

    /**
     * Обновление активности пользователя (для JavaScript heartbeat)
     */
    @PostMapping("/api/user/activity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateActivity(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long currentUserId = getCurrentUserId(request);

            if (currentUserId != null) {
                // Обновляем время последней активности
                userOnlineStatusService.updateLastSeen(currentUserId);

                response.put("success", true);
                response.put("message", "Activity updated");
                response.put("userId", currentUserId);
                response.put("timestamp", LocalDateTime.now());

                log.debug("Обновлена активность пользователя: {}", currentUserId);
                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("message", "User not authenticated");

                log.warn("Попытка обновления активности неаутентифицированного пользователя");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            log.error("Ошибка обновления активности: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Server error");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Установить пользователя офлайн (для beforeunload)
     */
    @PostMapping("/api/user/offline")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setUserOffline(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long currentUserId = getCurrentUserId(request);

            if (currentUserId != null) {
                userOnlineStatusService.setUserOffline(currentUserId);

                response.put("success", true);
                response.put("message", "User set offline");
                response.put("userId", currentUserId);

                log.info("Пользователь {} отмечен как офлайн", currentUserId);
                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("message", "User not authenticated");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            log.error("Ошибка установки офлайн статуса: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Получить текущий онлайн статус пользователя
     */
    @GetMapping("/api/user/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentUserStatus(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long currentUserId = getCurrentUserId(request);

            if (currentUserId != null) {
                boolean isOnline = userOnlineStatusService.isUserOnline(currentUserId);
                LocalDateTime lastSeen = userOnlineStatusService.getLastSeen(currentUserId);

                response.put("success", true);
                response.put("userId", currentUserId);
                response.put("isOnline", isOnline);
                response.put("lastSeen", lastSeen);

                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("message", "User not authenticated");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            log.error("Ошибка получения статуса: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Получить статус другого пользователя
     */
    @GetMapping("/api/user/{userId}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean isOnline = userOnlineStatusService.isUserOnline(userId);
            LocalDateTime lastSeen = userOnlineStatusService.getLastSeen(userId);

            response.put("success", true);
            response.put("userId", userId);
            response.put("isOnline", isOnline);
            response.put("lastSeen", lastSeen);
            response.put("statusText", isOnline ? "в сети" : "не в сети");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка получения статуса пользователя {}: {}", userId, e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ====================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ======================

    /**
     * Получить ID текущего пользователя из сессии
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            // Способ 1: Из сессии
            Object userId = request.getSession().getAttribute("currentUserId");
            if (userId instanceof Long) {
                return (Long) userId;
            }

            // Способ 2: Из Spring Security (если используете)
            /*
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                if (auth.getPrincipal() instanceof UserDetails) {
                    UserDetails userDetails = (UserDetails) auth.getPrincipal();
                    return Long.valueOf(userDetails.getUsername()); // если username содержит ID
                }
                // Или если principal уже содержит ID:
                return Long.valueOf(auth.getName());
            }
            */

            // Способ 3: Из JWT токена (если используете)
            /*
            String token = extractTokenFromRequest(request);
            if (token != null) {
                return jwtService.getUserIdFromToken(token);
            }
            */

            return null;

        } catch (Exception e) {
            log.warn("Ошибка получения ID пользователя: {}", e.getMessage());
            return null;
        }
    }
}
