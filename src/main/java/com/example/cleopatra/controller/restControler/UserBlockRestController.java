package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.BlockedUse.UserBlockResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.UserBlockService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
@Slf4j
public class UserBlockRestController {

    private final UserBlockService userBlockService;
    private final UserService userService;

    /**
     * Заблокировать пользователя (для JavaScript)
     */
    @PostMapping("/block/{userId}")
    public ResponseEntity<Map<String, Object>> blockUser(
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            User currentUser = userService.getCurrentUserEntity(authentication);
            UserBlockResponse response = userBlockService.blockUser(currentUser.getId(), userId);

            User blockedUser = userService.findById(userId);
            String message = String.format("Пользователь %s %s успешно заблокирован",
                    blockedUser.getFirstName(), blockedUser.getLastName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message,
                    "data", response
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "error", "Пользователь уже заблокирован"
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Ошибка при блокировке пользователя {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Произошла ошибка при блокировке пользователя"
                    ));
        }
    }

    /**
     * Разблокировать пользователя (для JavaScript)
     */
    @DeleteMapping("/unblock/{userId}")
    public ResponseEntity<Map<String, Object>> unblockUser(
            @PathVariable Long userId,
            Authentication authentication) {

        log.info("Попытка разблокировать пользователя с ID: {}", userId);

        try {
            // 1. Проверяем аутентификацию
            if (authentication == null) {
                log.error("Authentication is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "success", false,
                                "error", "Пользователь не аутентифицирован"
                        ));
            }

            log.info("Authentication name: {}", authentication.getName());

            // 2. Получаем текущего пользователя
            User currentUser;
            try {
                currentUser = userService.getCurrentUserEntity(authentication);
                log.info("Текущий пользователь получен: ID={}, Email={}",
                        currentUser.getId(), currentUser.getEmail());
            } catch (Exception e) {
                log.error("Ошибка при получении текущего пользователя", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "error", "Ошибка при получении текущего пользователя: " + e.getMessage()
                        ));
            }

            // 3. Проверяем существование пользователя для разблокировки
            User userToUnblock;
            try {
                userToUnblock = userService.findById(userId);
                log.info("Пользователь для разблокировки найден: ID={}, Email={}",
                        userToUnblock.getId(), userToUnblock.getEmail());
            } catch (Exception e) {
                log.error("Пользователь с ID {} не найден", userId, e);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "error", "Пользователь не найден"
                        ));
            }

            // 4. Выполняем разблокировку
            try {
                log.info("Вызываем userBlockService.unblockUser({}, {})", currentUser.getId(), userId);
                userBlockService.unblockUser(currentUser.getId(), userId);
                log.info("Разблокировка выполнена успешно");
            } catch (IllegalStateException e) {
                log.warn("Блокировка не найдена для пользователей {} -> {}", currentUser.getId(), userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "error", "Блокировка не найдена"
                        ));
            } catch (Exception e) {
                log.error("Ошибка при выполнении разблокировки", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "error", "Ошибка при разблокировке: " + e.getMessage()
                        ));
            }

            // 5. Формируем успешный ответ
            String message = String.format("Пользователь %s %s успешно разблокирован",
                    userToUnblock.getFirstName(), userToUnblock.getLastName());

            log.info("Разблокировка завершена успешно: {}", message);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message
            ));

        } catch (Exception e) {
            log.error("Неожиданная ошибка при разблокировке пользователя {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Произошла неожиданная ошибка: " + e.getClass().getSimpleName() + " - " + e.getMessage()
                    ));
        }
    }

    /**
     * Проверить статус блокировки
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> checkBlockStatus(
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            User currentUser = userService.getCurrentUserEntity(authentication);
            boolean isBlocked = userBlockService.isBlocked(currentUser.getId(), userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Статус блокировки получен",
                    "data", isBlocked
            ));

        } catch (Exception e) {
            log.error("Ошибка при проверке статуса блокировки для пользователя {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Ошибка при проверке статуса"
                    ));
        }
    }
}