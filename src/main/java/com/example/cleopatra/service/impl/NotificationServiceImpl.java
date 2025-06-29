package com.example.cleopatra.service.impl;

import com.example.cleopatra.EVENT.NotificationCreatedEvent;
import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.enums.NotificationType;
import com.example.cleopatra.maper.NotificationMapper;
import com.example.cleopatra.model.Notification;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.NotificationRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional

public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void createProfileVisitNotification(Long visitedUserId, Long visitorId) {

        try {
            // Получаем пользователей
            log.debug("🔍 Finding users: visitedUserId={}, visitorId={}", visitedUserId, visitorId);

            User visitedUser = userRepository.findById(visitedUserId)
                    .orElseThrow(() -> new RuntimeException("Visited user not found: " + visitedUserId));
            User visitor = userRepository.findById(visitorId)
                    .orElseThrow(() -> new RuntimeException("Visitor not found: " + visitorId));

            // Проверяем настройки приватности (null = разрешено)
            Boolean receiveNotifications = visitedUser.getReceiveVisitNotifications();

            // 🔧 ТОЛЬКО если явно отключено
            if (Boolean.FALSE.equals(receiveNotifications)) {
                return;
            }


            if (!Boolean.TRUE.equals(receiveNotifications)) {
                return;
            }

            // Проверяем, что нет недавнего аналогичного уведомления
            LocalDateTime recentThreshold = LocalDateTime.now().minusHours(1);

            boolean recentNotificationExists = notificationRepository.existsByRecipientIdAndActorIdAndTypeAndCreatedAtAfter(
                    visitedUserId, visitorId, NotificationType.PROFILE_VISIT, recentThreshold);
            if (recentNotificationExists) {
                return;
            }

            // Формируем сообщение
            String visitorName = getFullName(visitor);
            String title = "Посещение профиля";
            String message = String.format("%s посетил ваш профиль", visitorName);


            // Дополнительные данные в JSON
            String data = String.format("{\"visitorImageUrl\":\"%s\",\"profileUrl\":\"/profile/%d\"}",
                    visitor.getImageUrl() != null ? visitor.getImageUrl() : "", visitorId);

            Notification notification = createNotification(
                    visitedUser, visitor, NotificationType.PROFILE_VISIT,
                    title, message, data, visitorId, "USER"
            );


        } catch (Exception e) {
            log.error("❌ Error creating profile visit notification", e);
            e.printStackTrace();
        }
    }



    @Override
    public void createLikeNotification(Long postOwnerId, Long likerId, Long postId, String postTitle) {

        try {
            // Не уведомляем, если лайк поставил сам автор
            if (Objects.equals(postOwnerId, likerId)) {
                return;
            }

            User postOwner = userRepository.findById(postOwnerId)
                    .orElseThrow(() -> new RuntimeException("Post owner not found: " + postOwnerId));
            User liker = userRepository.findById(likerId)
                    .orElseThrow(() -> new RuntimeException("Liker not found: " + likerId));

            String likerName = getFullName(liker);
            String title = "Новый лайк";
            String message = String.format("%s лайкнул ваш пост \"%s\"",
                    likerName, truncateText(postTitle, 50));

            String data = String.format("{\"postId\":%d,\"likerImageUrl\":\"%s\",\"postUrl\":\"/posts/%d\"}",
                    postId, liker.getImageUrl() != null ? liker.getImageUrl() : "", postId);

            createNotification(
                    postOwner, liker, NotificationType.POST_LIKE,
                    title, message, data, postId, "POST"
            );


        } catch (Exception e) {
            log.error("❌ Error creating like notification", e);
        }
    }

    @Override
    public void createCommentNotification(Long postOwnerId, Long commenterId, Long postId, String commentText) {
        log.debug("Creating comment notification: commenter={}, postOwner={}, post={}", commenterId, postOwnerId, postId);

        try {
            if (Objects.equals(postOwnerId, commenterId)) {
                return;
            }



            User postOwner = userRepository.findById(postOwnerId)
                    .orElseThrow(() -> new RuntimeException("Post owner not found: " + postOwnerId));
            User commenter = userRepository.findById(commenterId)
                    .orElseThrow(() -> new RuntimeException("Commenter not found: " + commenterId));

            String commenterName = getFullName(commenter);
            String title = "Новый комментарий";
            String message = String.format("%s прокомментировал ваш пост: \"%s\"",
                    commenterName, truncateText(commentText, 100));

            String data = String.format("{\"postId\":%d,\"commenterId\":%d,\"commenterImageUrl\":\"%s\",\"postUrl\":\"/posts/%d\"}",
                    postId, commenterId, commenter.getImageUrl() != null ? commenter.getImageUrl() : "", postId);


            createNotification(
                    postOwner, commenter, NotificationType.POST_COMMENT,
                    title, message, data, postId, "POST"
            );

            log.info("✅ Created comment notification for post: {}", postId);

        } catch (Exception e) {
            log.error("❌ Error creating comment notification", e);
        }
    }

    @Override
    public void createFollowNotification(Long followedUserId, Long followerId) {
        log.debug("Creating follow notification: follower={}, followed={}", followerId, followedUserId);

        try {
            User followedUser = userRepository.findById(followedUserId)
                    .orElseThrow(() -> new RuntimeException("Followed user not found: " + followedUserId));
            User follower = userRepository.findById(followerId)
                    .orElseThrow(() -> new RuntimeException("Follower not found: " + followerId));

            String followerName = getFullName(follower);
            String title = "Новый подписчик";
            String message = String.format("%s подписался на вас", followerName);

            String data = String.format("{\"followerId\":%d,\"followerImageUrl\":\"%s\",\"profileUrl\":\"/profile/%d\"}",
                    followerId, follower.getImageUrl() != null ? follower.getImageUrl() : "", followerId);

            createNotification(
                    followedUser, follower, NotificationType.FOLLOW,
                    title, message, data, followerId, "USER"
            );

            log.info("✅ Created follow notification: {} followed {}", followerId, followedUserId);

        } catch (Exception e) {
            log.error("❌ Error creating follow notification", e);
        }
    }

    @Override
    public void createSystemNotification(Long userId, String title, String message) {
        log.debug("Creating system notification for user: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            createNotification(
                    user, null, NotificationType.SYSTEM_ANNOUNCEMENT,
                    title, message, null, null, "SYSTEM"
            );

            log.info("✅ Created system notification for user: {}", userId);

        } catch (Exception e) {
            log.error("❌ Error creating system notification", e);
        }
    }

    @Override
    public void createUnsubscribe(Long followedUserId, Long followerId) {

    }

    @Override
    public void createWallPostNotification(Long wallOwnerId, Long authorId, Long postId,
                                           String postText, String postPicUrl) {
        log.debug("Creating wall post notification: author={}, wallOwner={}, post={}",
                authorId, wallOwnerId, postId);

        try {
            User wallOwner = userRepository.findById(wallOwnerId)
                    .orElseThrow(() -> new RuntimeException("Wall owner not found: " + wallOwnerId));
            User author = userRepository.findById(authorId)
                    .orElseThrow(() -> new RuntimeException("Post author not found: " + authorId));

            String authorName = getFullName(author);
            String title = "Новая запись на стене";

            // Обрезаем текст поста для уведомления (максимум 50 символов)
            String truncatedText = postText != null && postText.length() > 50
                    ? postText.substring(0, 50) + "..."
                    : postText;

            String message = String.format("%s оставил(а) запись на вашей стене%s",
                    authorName,
                    truncatedText != null && !truncatedText.isEmpty()
                            ? ": \"" + truncatedText + "\""
                            : "");

            String data = String.format(
                    "{\"postId\":%d,\"authorId\":%d,\"authorImageUrl\":\"%s\",\"postUrl\":\"/wall/%d\",\"hasImage\":%s}",
                    postId,
                    authorId,
                    author.getImageUrl() != null ? author.getImageUrl() : "",
                    wallOwnerId,  // просто на стену владельца
                    postPicUrl != null && !postPicUrl.isEmpty()
            );

            createNotification(
                    wallOwner, author, NotificationType.WALL_POST,
                    title, message, data, postId, "WALL_POST"
            );

            log.info("✅ Created wall post notification: {} posted on {}'s wall", authorId, wallOwnerId);

        } catch (Exception e) {
            log.error("❌ Error creating wall post notification", e);
        }
    }


    // ===================== ПОЛУЧЕНИЕ УВЕДОМЛЕНИЙ =====================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        log.debug("Getting notifications for user: {}", userId);

        try {
            Page<Notification> notifications = notificationRepository.findByRecipientId(userId, pageable);
            return notifications.map(notificationMapper::toDto);

        } catch (Exception e) {
            log.error("❌ Error getting user notifications for user: {}", userId, e);
            return Page.empty(pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        log.debug("Getting unread notifications for user: {}", userId);

        try {
            List<Notification> notifications = notificationRepository.findUnreadByRecipientId(userId);
            return notifications.stream()
                    .map(notificationMapper::toDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error getting unread notifications for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        log.debug("Getting unread count for user: {}", userId);

        try {
            return notificationRepository.countUnreadByRecipientId(userId);

        } catch (Exception e) {
            log.error("❌ Error getting unread count for user: {}", userId, e);
            return 0;
        }
    }

    @Override
    public Page<NotificationDto> getUnreadNotificationsWithPagination(Long userId, Pageable pageable) {
        try {
            log.debug("Getting unread notifications for user: {} with pagination", userId);

            Page<Notification> notifications = notificationRepository
                    .findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);

            log.debug("Found {} unread notifications for user: {}",
                    notifications.getTotalElements(), userId);

            return notifications.map(notificationMapper::toDto);

        } catch (Exception e) {
            log.error("Error getting unread notifications with pagination for user: {}", userId, e);
            throw new RuntimeException("Failed to get unread notifications", e);
        }
    }

    // ===================== УПРАВЛЕНИЕ УВЕДОМЛЕНИЯМИ =====================

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        log.debug("Marking notification as read: id={}, user={}", notificationId, userId);

        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

            // Проверяем права доступа
            if (!notification.getRecipient().getId().equals(userId)) {
                throw new SecurityException("Access denied to notification: " + notificationId);
            }

            if (!notification.getIsRead()) {
                notification.setIsRead(true);
                notification.setReadAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.info("✅ Marked notification as read: {}", notificationId);
            }

        } catch (Exception e) {
            log.error("❌ Error marking notification as read: {}", notificationId, e);
            throw e;
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        log.debug("Marking all notifications as read for user: {}", userId);

        try {
            int updatedCount = notificationRepository.markAllAsReadByRecipientId(userId);
            log.info("✅ Marked {} notifications as read for user: {}", updatedCount, userId);

        } catch (Exception e) {
            log.error("❌ Error marking all notifications as read for user: {}", userId, e);
            throw e;
        }
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        log.debug("Deleting notification: id={}, user={}", notificationId, userId);

        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

            if (!notification.getRecipient().getId().equals(userId)) {
                throw new SecurityException("Access denied to notification: " + notificationId);
            }

            notificationRepository.delete(notification);
            log.info("✅ Deleted notification: {}", notificationId);

        } catch (Exception e) {
            log.error("❌ Error deleting notification: {}", notificationId, e);
            throw e;
        }
    }

    @Override
    public void sendPendingNotifications() {

    }

    @Override
    public void cleanupOldNotifications() {

    }


    @Override
    public void deleteAllNotifications(Long userId) {
        try {
            notificationRepository.deleteByRecipientId(userId);
        } catch (Exception e) {
            log.error("Error deleting notifications for user: " + userId, e);
            throw new RuntimeException("Failed to delete notifications", e);
        }
    }


    @Override
    @Transactional
    public void createUnsubscribeNotification(Long recipientId, Long actorId, String actorName) {
        try {
            User recipient = userRepository.findById(recipientId)
                    .orElseThrow(() -> new EntityNotFoundException("Recipient not found: " + recipientId));

            User actor = userRepository.findById(actorId)
                    .orElseThrow(() -> new EntityNotFoundException("Actor not found: " + actorId));

            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(NotificationType.UNFOLLOW)
                    .title("Подписка отменена")
                    .message(actorName + " отписался от вас")
                    .isRead(false)
                    .isSent(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            log.info("Created UNFOLLOW notification: {} unsubscribed from {}", actorId, recipientId);

        } catch (Exception e) {
            log.error("Error creating UNFOLLOW notification: actor={}, recipient={}",
                    actorId, recipientId, e);
            throw new RuntimeException("Failed to create unfollow notification", e);
        }
    }

    // ===================== ПРИВАТНЫЕ МЕТОДЫ =====================

    private Notification createNotification(User recipient, User actor, NotificationType type,
                                            String title, String message, String data,
                                            Long relatedEntityId, String relatedEntityType) {

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .isRead(false)     // 🔧 Явно устанавливаем
                .isSent(false)     // 🔧 Явно устанавливаем
                .createdAt(LocalDateTime.now()) // 🔧 Явно устанавливаем
                .build();
        try {
            Notification saved = notificationRepository.save(notification);

            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    saved.getId(),
                    saved.getRecipient().getId()
            ));

            return saved;

        } catch (Exception e) {
            log.error("❌ Error saving notification to database", e);
            e.printStackTrace();
            throw e;
        }
    }

    private String getFullName(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName();
        } else if (user.getFirstName() != null) {
            return user.getFirstName();
        } else if (user.getLastName() != null) {
            return user.getLastName();
        } else {
            return user.getEmail(); // Fallback to email
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
