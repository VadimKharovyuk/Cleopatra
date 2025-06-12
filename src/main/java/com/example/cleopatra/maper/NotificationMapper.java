package com.example.cleopatra.maper;

import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.enums.NotificationType;
import com.example.cleopatra.model.Notification;
import com.example.cleopatra.model.User;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NotificationMapper {

    /**
     * Преобразование Notification в NotificationDto
     */
    public NotificationDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        try {
            return NotificationDto.builder()
                    .id(notification.getId())
                    .recipientId(notification.getRecipient() != null ? notification.getRecipient().getId() : null)
                    .actorId(notification.getActor() != null ? notification.getActor().getId() : null)
                    .actorName(getActorName(notification))
                    .actorImageUrl(getActorImageUrl(notification))
                    .type(notification.getType())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .data(notification.getData())
                    .relatedEntityId(notification.getRelatedEntityId())
                    .relatedEntityType(notification.getRelatedEntityType())
                    .isRead(notification.getIsRead())
                    .relatedEntityId(notification.getRelatedEntityId())      // ✅ Передаем ID поста
                    .relatedEntityType(notification.getRelatedEntityType())
                    .isSent(notification.getIsSent())
                    .sentAt(notification.getSentAt())
                    .readAt(notification.getReadAt())
                    .createdAt(notification.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error mapping notification to DTO: {}", notification.getId(), e);
            return createErrorDto(notification);
        }
    }


    /**
     * Создание DTO для WebSocket уведомлений (минимальная информация)
     */
    public NotificationDto toWebSocketDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        try {
            return NotificationDto.builder()
                    .id(notification.getId())
                    .actorName(getActorName(notification))
                    .actorImageUrl(getActorImageUrl(notification))
                    .type(notification.getType())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .data(notification.getData())
                    .relatedEntityId(notification.getRelatedEntityId())      // ✅ Передаем ID поста
                    .relatedEntityType(notification.getRelatedEntityType())
                    .relatedEntityId(notification.getRelatedEntityId())
                    .relatedEntityType(notification.getRelatedEntityType())
                    .createdAt(notification.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error mapping notification for WebSocket: {}", notification.getId(), e);
            return null;
        }
    }


    // ===================== ПРИВАТНЫЕ МЕТОДЫ =====================

    /**
     * Получение полного имени актора
     */
    private String getActorName(Notification notification) {
        if (notification == null || notification.getActor() == null) {
            return "Система"; // Для системных уведомлений
        }

        try {
            User actor = notification.getActor();

            // Собираем полное имя
            StringBuilder nameBuilder = new StringBuilder();

            if (actor.getFirstName() != null && !actor.getFirstName().trim().isEmpty()) {
                nameBuilder.append(actor.getFirstName().trim());
            }

            if (actor.getLastName() != null && !actor.getLastName().trim().isEmpty()) {
                if (!nameBuilder.isEmpty()) {
                    nameBuilder.append(" ");
                }
                nameBuilder.append(actor.getLastName().trim());
            }

            // Если имя не указано, используем email
            if (nameBuilder.isEmpty()) {
                if (actor.getEmail() != null && !actor.getEmail().trim().isEmpty()) {
                    String email = actor.getEmail().trim();
                    // Берем часть до @ для отображения
                    int atIndex = email.indexOf('@');
                    return atIndex > 0 ? email.substring(0, atIndex) : email;
                } else {
                    return "Пользователь #" + actor.getId();
                }
            }

            return nameBuilder.toString();

        } catch (Exception e) {
            log.warn("⚠️ Error getting actor name for notification: {}", notification.getId(), e);
            return "Неизвестный пользователь";
        }
    }

    /**
     * Получение URL изображения актора
     */
    private String getActorImageUrl(Notification notification) {
        if (notification == null || notification.getActor() == null) {
            return null;
        }

        try {
            // 🔥 Проверяем, инициализирован ли прокси
            if (!Hibernate.isInitialized(notification.getActor())) {
                log.warn("⚠️ Actor not initialized for notification: {}", notification.getId());
                return null;
            }

            String imageUrl = notification.getActor().getImageUrl();

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                return imageUrl.trim();
            }

            return null;

        } catch (Exception e) {
            log.warn("⚠️ Error getting actor image URL for notification: {}", notification.getId(), e);
            return null;
        }
    }

    /**
     * Обрезка сообщения до указанной длины
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return "";
        }

        if (message.length() <= maxLength) {
            return message;
        }

        return message.substring(0, maxLength).trim() + "...";
    }

    /**
     * Создание DTO с минимальной информацией в случае ошибки
     */
    private NotificationDto createErrorDto(Notification notification) {
        try {
            return NotificationDto.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .title("Ошибка загрузки")
                    .message("Не удалось загрузить уведомление")
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("❌ Critical error creating error DTO for notification", e);

            // Последняя попытка создать хоть что-то
            return NotificationDto.builder()
                    .id(notification != null ? notification.getId() : -1L)
                    .title("Ошибка")
                    .message("Критическая ошибка загрузки")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }
}