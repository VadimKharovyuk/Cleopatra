package com.example.cleopatra.controller;
import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.service.NotificationService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * Главная страница уведомлений
     */
    @GetMapping
    public String getNotificationsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            Model model) {

        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationDto> notifications = notificationService.getUserNotifications(userId, pageable);
            long unreadCount = notificationService.getUnreadCount(userId);

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", notifications.getTotalPages());
            model.addAttribute("hasNext", notifications.hasNext());
            model.addAttribute("hasPrevious", notifications.hasPrevious());
            model.addAttribute("currentUserId", userId); // 🆕 Для WebSocket
            model.addAttribute("isUnreadOnly", false);

            return "notifications/list";

        } catch (Exception e) {
            log.error("❌ Error loading notifications page", e);
            model.addAttribute("error", "Не удалось загрузить уведомления");
            return "error";
        }
    }

    /**
     * Страница непрочитанных уведомлений
     */
    @GetMapping("/unread")
    public String getUnreadNotificationsPage(Authentication authentication, Model model) {
        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            var unreadNotifications = notificationService.getUnreadNotifications(userId);
            long unreadCount = notificationService.getUnreadCount(userId);

            model.addAttribute("notifications", unreadNotifications);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("isUnreadOnly", true);

            return "notifications/list";

        } catch (Exception e) {
            log.error("❌ Error loading unread notifications", e);
            model.addAttribute("error", "Не удалось загрузить непрочитанные уведомления");
            return "error";
        }
    }

    /**
     * Пометить уведомление как прочитанное (POST из формы)
     */
    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            notificationService.markAsRead(id, userId);
            redirectAttributes.addFlashAttribute("success", "Уведомление помечено как прочитанное");

        } catch (Exception e) {
            log.error("❌ Error marking notification as read: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Не удалось пометить уведомление как прочитанное");
        }

        return "redirect:/notifications";
    }

    /**
     * Пометить все уведомления как прочитанные
     */
    @PostMapping("/mark-all-read")
    public String markAllAsRead(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            notificationService.markAllAsRead(userId);
            redirectAttributes.addFlashAttribute("success", "Все уведомления помечены как прочитанные");

        } catch (Exception e) {
            log.error("❌ Error marking all notifications as read", e);
            redirectAttributes.addFlashAttribute("error", "Не удалось пометить все уведомления как прочитанные");
        }

        return "redirect:/notifications";
    }

    /**
     * Удалить уведомление
     */
    @PostMapping("/{id}/delete")
    public String deleteNotification(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String email = authentication.getName();
            Long userId = userService.getUserIdByEmail(email);

            notificationService.deleteNotification(id, userId);
            redirectAttributes.addFlashAttribute("success", "Уведомление удалено");

        } catch (Exception e) {
            log.error("❌ Error deleting notification: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Не удалось удалить уведомление");
        }

        return "redirect:/notifications";
    }

//    /**
//     * Страница настроек уведомлений
//     */
//    @GetMapping("/settings")
//    public String getNotificationSettings(Authentication authentication, Model model) {
//        try {
//            String email = authentication.getName();
//            Long userId = userService.getUserIdByEmail(email);
//
//            // Получаем текущие настройки пользователя
//            var user = userService.findById(userId);
//            model.addAttribute("receiveVisitNotifications",
//                    user.getReceiveVisitNotifications() != null ? user.getReceiveVisitNotifications() : true);
//
//            return "notifications/settings"; // notifications/settings.html
//
//        } catch (Exception e) {
//            log.error("❌ Error loading notification settings", e);
//            model.addAttribute("error", "Не удалось загрузить настройки");
//            return "error";
//        }
//    }
}
