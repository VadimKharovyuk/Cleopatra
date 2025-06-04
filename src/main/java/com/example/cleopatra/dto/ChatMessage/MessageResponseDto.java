package com.example.cleopatra.dto.ChatMessage;

import com.example.cleopatra.enums.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponseDto {

    private Long id;

    // Информация об отправителе
    private UserBriefDto sender;

    // Информация о получателе
    private UserBriefDto recipient;

    // Содержимое сообщения
    private String content;

    // Статусы
    private Boolean isRead;
    private Boolean isEdited;
    private Boolean deletedBySender;
    private Boolean deletedByRecipient;

    // Время прочтения
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    // Сообщение на которое отвечаем
    private MessageBriefDto replyToMessage;

    // Статус доставки
    private DeliveryStatus deliveryStatus;
    private String deliveryStatusText;

    // Временные метки
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Дополнительные поля для удобства фронтенда
    private Boolean isOwnMessage; // Отправлено ли текущим пользователем
    private Boolean canEdit;      // Можно ли редактировать
    private Boolean canDelete;    // Можно ли удалить
    private String timeAgo;       // "5 минут назад"
}

