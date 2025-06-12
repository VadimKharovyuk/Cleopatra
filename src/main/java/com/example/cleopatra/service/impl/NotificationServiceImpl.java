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
        log.info("🔔 START createProfileVisitNotification: visitor={}, visited={}", visitorId, visitedUserId);

        try {
            // Получаем пользователей
            log.debug("🔍 Finding users: visitedUserId={}, visitorId={}", visitedUserId, visitorId);

            User visitedUser = userRepository.findById(visitedUserId)
                    .orElseThrow(() -> new RuntimeException("Visited user not found: " + visitedUserId));
            User visitor = userRepository.findById(visitorId)
                    .orElseThrow(() -> new RuntimeException("Visitor not found: " + visitorId));

            log.info("✅ Users found: visited={}, visitor={}", visitedUser.getEmail(), visitor.getEmail());

            // Проверяем настройки приватности (null = разрешено)
            Boolean receiveNotifications = visitedUser.getReceiveVisitNotifications();
            log.info("🔒 Privacy check: user {} receiveVisitNotifications = {}", visitedUserId, receiveNotifications);

            // 🔧 ТОЛЬКО если явно отключено
            if (Boolean.FALSE.equals(receiveNotifications)) {
                log.info("🚫 User {} disabled visit notifications, skipping", visitedUserId);
                return;
            }

            log.info("✅ Visit notifications enabled for user {}, proceeding...", visitedUserId);

            if (!Boolean.TRUE.equals(receiveNotifications)) {
                log.info("🚫 User {} disabled visit notifications, skipping", visitedUserId);
                return;
            }

            // Проверяем, что нет недавнего аналогичного уведомления
            LocalDateTime recentThreshold = LocalDateTime.now().minusHours(1);
            log.debug("🕐 Checking for recent notifications since: {}", recentThreshold);

            boolean recentNotificationExists = notificationRepository.existsByRecipientIdAndActorIdAndTypeAndCreatedAtAfter(
                    visitedUserId, visitorId, NotificationType.PROFILE_VISIT, recentThreshold);

            log.info("🔍 Recent notification exists: {}", recentNotificationExists);

            if (recentNotificationExists) {
                log.info("🚫 Recent profile visit notification already exists, skipping");
                return;
            }

            // Формируем сообщение
            String visitorName = getFullName(visitor);
            String title = "Посещение профиля";
            String message = String.format("%s посетил ваш профиль", visitorName);

            log.info("📝 Creating notification: title='{}', message='{}'", title, message);

            // Дополнительные данные в JSON
            String data = String.format("{\"visitorImageUrl\":\"%s\",\"profileUrl\":\"/profile/%d\"}",
                    visitor.getImageUrl() != null ? visitor.getImageUrl() : "", visitorId);

            // Создаем уведомление
            log.info("💾 Saving notification to database...");
            Notification notification = createNotification(
                    visitedUser, visitor, NotificationType.PROFILE_VISIT,
                    title, message, data, visitorId, "USER"
            );


            log.info("✅ Created profile visit notification: id={}", notification.getId());

        } catch (Exception e) {
            log.error("❌ Error creating profile visit notification", e);
            e.printStackTrace();
        }
    }



    @Override
    public void createLikeNotification(Long postOwnerId, Long likerId, Long postId, String postTitle) {
        log.debug("Creating like notification: liker={}, postOwner={}, post={}", likerId, postOwnerId, postId);

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

            String data = String.format("{\"postId\":%d,\"likerImageUrl\":\"%s\",\"postUrl\":\"/post/%d\"}",
                    postId, liker.getImageUrl() != null ? liker.getImageUrl() : "", postId);

            createNotification(
                    postOwner, liker, NotificationType.POST_LIKE,
                    title, message, data, postId, "POST"
            );

            log.info("✅ Created like notification for post: {}", postId);

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

            String data = String.format("{\"postId\":%d,\"commenterId\":%d,\"commenterImageUrl\":\"%s\",\"postUrl\":\"/post/%d\"}",
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
    @Transactional
    public void createMentionNotification(Long mentionedUserId, Long mentionerUserId) {
        try {
            log.info("Creating mention notification: mentioner {} → mentioned {}", mentionerUserId, mentionedUserId);

            // Проверяем валидность параметров
            if (mentionedUserId == null || mentionerUserId == null) {
                log.warn("Invalid parameters for mention notification: mentionedUserId={}, mentionerUserId={}",
                        mentionedUserId, mentionerUserId);
                return;
            }

            // Проверяем, что пользователь не упоминает сам себя
            if (mentionedUserId.equals(mentionerUserId)) {
                log.debug("User {} tried to mention themselves, skipping notification", mentionerUserId);
                return;
            }

            // Получаем пользователей из базы данных
            User mentionedUser = userRepository.findById(mentionedUserId)
                    .orElseThrow(() -> new RuntimeException("Упомянутый пользователь с ID " + mentionedUserId + " не найден"));

            User mentionerUser = userRepository.findById(mentionerUserId)
                    .orElseThrow(() -> new RuntimeException("Пользователь-автор с ID " + mentionerUserId + " не найден"));

            // Проверяем, не заблокирован ли пользователь
            // TODO: Добавить проверку блокировки если есть такая функциональность
            // if (isUserBlocked(mentionedUserId, mentionerUserId)) {
            //     log.debug("User {} is blocked by {}, skipping notification", mentionerUserId, mentionedUserId);
            //     return;
            // }

            // Формируем заголовок уведомления
            String title = String.format("%s %s упомянул вас в посте",
                    mentionerUser.getFirstName(),
                    mentionerUser.getLastName());

            // Формируем содержание уведомления
            String message = String.format("%s %s упомянул вас в своем посте",
                    mentionerUser.getFirstName(),
                    mentionerUser.getLastName());

            // Создаем уведомление
            Notification notification = Notification.builder()
                    .recipient(mentionedUser)
                    .actor(mentionerUser)
                    .type(NotificationType.MENTION)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .isSent(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Сохраняем уведомление
            Notification savedNotification = notificationRepository.save(notification);

            log.info("✅ Mention notification created successfully: ID={}, recipient={}, actor={}",
                    savedNotification.getId(), mentionedUserId, mentionerUserId);

            // Публикуем событие для отправки уведомления
            NotificationCreatedEvent event = new NotificationCreatedEvent(
                    savedNotification.getId(),
                    mentionedUserId
            );
            eventPublisher.publishEvent(event);

            log.info("📤 NotificationCreatedEvent published for mention notification: {}", savedNotification.getId());

        } catch (Exception e) {
            log.error("❌ Error creating mention notification: mentioner {} → mentioned {}: {}",
                    mentionerUserId, mentionedUserId, e.getMessage(), e);
            throw new RuntimeException("Не удалось создать уведомление об упоминании: " + e.getMessage(), e);
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
    // ===================== ПРИВАТНЫЕ МЕТОДЫ =====================

    private Notification createNotification(User recipient, User actor, NotificationType type,
                                            String title, String message, String data,
                                            Long relatedEntityId, String relatedEntityType) {

        log.info("🏗️ Building notification object...");

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

        log.info("💾 Saving notification to repository...");

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("✅ Notification saved successfully with ID: {}", saved.getId());

            log.info("📢 Publishing NotificationCreatedEvent for ID: {}", saved.getId());
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
