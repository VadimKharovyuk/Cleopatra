package com.example.cleopatra.controller.restControler;
import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.service.NotificationService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Для фронтенда
public class NotificationRestController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * Получить уведомления пользователя (пагинация)
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationDto> notifications = notificationService.getUserNotifications(userId, pageable);

            return ResponseEntity.ok(Map.of(
                    "notifications", notifications.getContent(),
                    "currentPage", page,
                    "totalPages", notifications.getTotalPages(),
                    "totalElements", notifications.getTotalElements(),
                    "hasNext", notifications.hasNext(),
                    "hasPrevious", notifications.hasPrevious()
            ));

        } catch (Exception e) {
            log.error("❌ Error getting notifications via API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get notifications"));
        }
    }

    /**
     * Получить непрочитанные уведомления
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            List<NotificationDto> notifications = notificationService.getUnreadNotifications(userId);

            return ResponseEntity.ok(Map.of(
                    "notifications", notifications,
                    "count", notifications.size()
            ));

        } catch (Exception e) {
            log.error("❌ Error getting unread notifications via API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get unread notifications"));
        }
    }

    /**
     * Получить количество непрочитанных уведомлений
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            long count = notificationService.getUnreadCount(userId);

            return ResponseEntity.ok(Map.of("unreadCount", count));

        } catch (Exception e) {
            log.error("❌ Error getting unread count via API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get unread count"));
        }
    }

    /**
     * Пометить уведомление как прочитанное
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            notificationService.markAsRead(id, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notification marked as read"
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        } catch (Exception e) {
            log.error("❌ Error marking notification as read via API: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark notification as read"));
        }
    }

    /**
     * Пометить все уведомления как прочитанные
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            notificationService.markAllAsRead(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "All notifications marked as read"
            ));

        } catch (Exception e) {
            log.error("❌ Error marking all notifications as read via API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark all notifications as read"));
        }
    }

    /**
     * Удалить уведомление
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            notificationService.deleteNotification(id, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notification deleted"
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        } catch (Exception e) {
            log.error("❌ Error deleting notification via API: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete notification"));
        }
    }

    @PostMapping("/notification")
    public ResponseEntity<String> testNotification(
            @RequestParam Long visitedUserId,
            @RequestParam Long visitorId) {

        try {
            notificationService.createProfileVisitNotification(visitedUserId, visitorId);
            return ResponseEntity.ok("Notification created");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
