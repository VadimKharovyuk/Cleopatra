package com.example.cleopatra.service;

import com.example.cleopatra.dto.Notification.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    // Создание уведомлений
    void createProfileVisitNotification(Long visitedUserId, Long visitorId);
    void createLikeNotification(Long postOwnerId, Long likerId, Long postId, String postTitle);
    void createCommentNotification(Long postOwnerId, Long commenterId, Long postId, String commentText);
    void createFollowNotification(Long followedUserId, Long followerId);
    void createSystemNotification(Long userId, String title, String message);
    void createUnsubscribe(Long followedUserId, Long followerId);



    // Получение уведомлений
    Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable);
    List<NotificationDto> getUnreadNotifications(Long userId);
    long getUnreadCount(Long userId);
    Page<NotificationDto> getUnreadNotificationsWithPagination(Long userId, Pageable pageable);


    // Управление уведомлениями
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
    void deleteNotification(Long notificationId, Long userId);

    // Служебные методы
    void sendPendingNotifications();
    void cleanupOldNotifications();

    void deleteAllNotifications(Long userId);

    void createUnsubscribeNotification(Long subscribedToId, Long subscriberId, String subscriberName);
}
