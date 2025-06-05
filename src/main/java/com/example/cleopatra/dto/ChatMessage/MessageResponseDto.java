package com.example.cleopatra.dto.ChatMessage;

import com.example.cleopatra.enums.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // Сообщение на которое отвечаем (переименовываем для совместимости)
    @JsonProperty("replyToMessage")
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

    // НОВЫЕ МЕТОДЫ для совместимости с JavaScript

    /**
     * Возвращает ISO string timestamp для JavaScript
     */
    @JsonProperty("timestamp")
    public String getTimestamp() {
        return createdAt != null ? createdAt.toString() : null;
    }

    /**
     * Геттер для совместимости с boolean типами в JavaScript
     */
    @JsonProperty("isOwnMessage")
    public boolean isOwnMessage() {
        return Boolean.TRUE.equals(this.isOwnMessage);
    }

    @JsonProperty("canEdit")
    public boolean canEdit() {
        return Boolean.TRUE.equals(this.canEdit);
    }

    @JsonProperty("canDelete")
    public boolean canDelete() {
        return Boolean.TRUE.equals(this.canDelete);
    }

    @JsonProperty("isEdited")
    public boolean isEdited() {
        return Boolean.TRUE.equals(this.isEdited);
    }

    /**
     * Метод для получения короткого контента для ответов
     */
    @JsonIgnore
    public String getShortContent() {
        if (content == null) return "";
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    /**
     * Проверка, является ли сообщение удаленным для текущего пользователя
     */
    @JsonIgnore
    public boolean isDeletedForUser(Long userId) {
        if (sender != null && sender.getId().equals(userId)) {
            return Boolean.TRUE.equals(deletedBySender);
        }
        if (recipient != null && recipient.getId().equals(userId)) {
            return Boolean.TRUE.equals(deletedByRecipient);
        }
        return false;
    }

    /**
     * Проверка доступности для редактирования
     */
    @JsonIgnore
    public boolean isEditableBy(Long userId) {
        return sender != null &&
                sender.getId().equals(userId) &&
                !Boolean.TRUE.equals(deletedBySender) &&
                deliveryStatus != DeliveryStatus.READ; // Пример ограничения
    }

    /**
     * Проверка доступности для удаления
     */
    @JsonIgnore
    public boolean isDeletableBy(Long userId) {
        return sender != null &&
                sender.getId().equals(userId) &&
                !Boolean.TRUE.equals(deletedBySender);
    }
}

