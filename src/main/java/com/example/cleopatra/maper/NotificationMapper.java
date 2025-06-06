package com.example.cleopatra.maper;

import com.example.cleopatra.dto.Notification.NotificationDto;
import com.example.cleopatra.enums.NotificationType;
import com.example.cleopatra.model.Notification;
import com.example.cleopatra.model.User;
import lombok.extern.slf4j.Slf4j;
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
     * Преобразование списка Notification в список NotificationDto
     */
    public List<NotificationDto> toDtoList(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return Collections.emptyList();
        }

        return notifications.stream()
                .map(this::toDto)
                .filter(Objects::nonNull) // Фильтруем null значения на случай ошибок
                .collect(Collectors.toList());
    }

    /**
     * Преобразование Page<Notification> в Page<NotificationDto>
     */
    public Page<NotificationDto> toDtoPage(Page<Notification> notificationPage) {
        if (notificationPage == null) {
            return Page.empty();
        }

        return notificationPage.map(this::toDto);
    }

    /**
     * Создание краткого DTO для списков (без полного контента)
     */
    public NotificationDto toSummaryDto(Notification notification) {
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
                    .message(truncateMessage(notification.getMessage(), 100))
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error mapping notification summary: {}", notification.getId(), e);
            return null;
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
                    .relatedEntityId(notification.getRelatedEntityId())
                    .relatedEntityType(notification.getRelatedEntityType())
                    .createdAt(notification.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error mapping notification for WebSocket: {}", notification.getId(), e);
            return null;
        }
    }

    /**
     * Создание DTO с полной информацией для детального просмотра
     */
    public NotificationDto toDetailDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        try {
            NotificationDto dto = toDto(notification);

            // Добавляем дополнительную информацию
            if (notification.getActor() != null) {
                dto.setActorImageUrl(notification.getActor().getImageUrl());
                // Можно добавить еще поля если нужно
            }

            return dto;

        } catch (Exception e) {
            log.error("❌ Error mapping detailed notification: {}", notification.getId(), e);
            return createErrorDto(notification);
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
                if (nameBuilder.length() > 0) {
                    nameBuilder.append(" ");
                }
                nameBuilder.append(actor.getLastName().trim());
            }

            // Если имя не указано, используем email
            if (nameBuilder.length() == 0) {
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
            return null; // Или путь к дефолтной аватарке системы
        }

        try {
            String imageUrl = notification.getActor().getImageUrl();

            // Проверяем что URL не пустой
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                return imageUrl.trim();
            }

            return null; // Frontend подставит дефолтную аватарку

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

    /**
     * Валидация notification перед маппингом
     */
    private boolean isValidNotification(Notification notification) {
        if (notification == null) {
            return false;
        }

        // Проверяем обязательные поля
        return notification.getId() != null &&
                notification.getRecipient() != null &&
                notification.getType() != null &&
                notification.getTitle() != null &&
                notification.getMessage() != null;
    }

    /**
     * Форматирование времени для отображения (опционально)
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(dateTime, now);

            long seconds = duration.getSeconds();

            if (seconds < 60) {
                return "только что";
            } else if (seconds < 3600) {
                long minutes = seconds / 60;
                return minutes + " мин. назад";
            } else if (seconds < 86400) {
                long hours = seconds / 3600;
                return hours + " ч. назад";
            } else {
                long days = seconds / 86400;
                if (days == 1) {
                    return "вчера";
                } else if (days < 7) {
                    return days + " дн. назад";
                } else {
                    // Для старых уведомлений показываем дату
                    return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                }
            }

        } catch (Exception e) {
            log.warn("⚠️ Error formatting time ago", e);
            return "";
        }
    }

    /**
     * Получение иконки для типа уведомления
     */
    private String getTypeIcon(NotificationType type) {
        if (type == null) {
            return "🔔";
        }

        return type.getEmoji() != null ? type.getEmoji() : "🔔";
    }

    /**
     * Получение цвета для типа уведомления (для UI)
     */
    private String getTypeColor(NotificationType type) {
        if (type == null) {
            return "gray";
        }

        switch (type) {
            case PROFILE_VISIT:
                return "blue";
            case FOLLOW:
                return "green";
            case POST_LIKE:
                return "red";
            case POST_COMMENT:
                return "orange";
            case COMMENT_LIKE:
                return "purple";
            case SYSTEM_ANNOUNCEMENT:
                return "yellow";
            default:
                return "gray";
        }
    }

    // В класс NotificationMapper добавьте эти методы:

    /**
     * Маппинг для мобильного API (упрощенная версия)
     */
    public NotificationDto toMobileDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationDto.builder()
                .id(notification.getId())
                .actorName(getActorName(notification))
                .actorImageUrl(getActorImageUrl(notification))
                .type(notification.getType())
                .title(notification.getTitle())
                .message(truncateMessage(notification.getMessage(), 80)) // Короче для мобильных
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }



    /**
     * Маппинг для email уведомлений
     */
    public NotificationDto toEmailDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        NotificationDto dto = toDto(notification);

        // Добавляем дополнительную информацию для email
        if (notification.getType() == NotificationType.PROFILE_VISIT) {
            dto.setMessage(dto.getMessage() + ". Посмотрите кто еще заходил к вам на страницу!");
        }

        return dto;
    }

    /**
     * Создание DTO из данных без entity (для тестов)
     */
    public NotificationDto createDto(Long id, String actorName, NotificationType type,
                                     String title, String message, boolean isRead,
                                     LocalDateTime createdAt) {
        return NotificationDto.builder()
                .id(id)
                .actorName(actorName)
                .type(type)
                .title(title)
                .message(message)
                .isRead(isRead)
                .createdAt(createdAt)
                .build();
    }
}
