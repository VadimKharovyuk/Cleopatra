package com.example.cleopatra.maper;

import com.example.cleopatra.dto.ChatMessage.*;
import com.example.cleopatra.model.Message;
import com.example.cleopatra.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MessageMapper {

    /**
     * Преобразование Message -> MessageResponseDto
     */
    public MessageResponseDto toResponseDto(Message message) {
        if (message == null) {
            return null;
        }

        return MessageResponseDto.builder()
                .id(message.getId())
                .sender(toUserBriefDto(message.getSender()))
                .recipient(toUserBriefDto(message.getRecipient()))
                .content(message.getContent())
                .isRead(message.getIsRead())
                .isEdited(message.getIsEdited())
                .deletedBySender(message.getDeletedBySender())
                .deletedByRecipient(message.getDeletedByRecipient())
                .readAt(message.getReadAt())
                .replyToMessage(toBriefDto(message.getReplyToMessage()))
                .deliveryStatus(message.getDeliveryStatus())
                .deliveryStatusText(message.getDeliveryStatus() != null ?
                        message.getDeliveryStatus().getDisplayName() : null)
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    /**
     * Преобразование Message -> MessageResponseDto с дополнительными полями для текущего пользователя
     */
    public MessageResponseDto toResponseDto(Message message, Long currentUserId) {
        MessageResponseDto dto = toResponseDto(message);
        if (dto != null) {
            enrichMessageDto(dto, currentUserId);
        }
        return dto;
    }

    /**
     * Преобразование Message -> MessageBriefDto
     */
    public MessageBriefDto toBriefDto(Message message) {
        if (message == null) {
            return null;
        }

        return MessageBriefDto.builder()
                .id(message.getId())
                .sender(toUserBriefDto(message.getSender()))
                .content(message.getContent())
                .shortContent(truncateContent(message.getContent()))
                .build();
    }

    /**
     * Преобразование User -> UserBriefDto
     */
    public UserBriefDto toUserBriefDto(User user) {
        if (user == null) {
            return null;
        }

        return UserBriefDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(buildFullName(user.getFirstName(), user.getLastName()))
                .imageUrl(user.getImageUrl())
                .isOnline(null) // Будет заполнено отдельно
                .lastSeenText(null) // Будет заполнено отдельно
                .build();
    }

    /**
     * Создание сущности Message из MessageCreateDto
     */
    public Message toEntity(MessageCreateDto dto) {
        if (dto == null) {
            return null;
        }

        return Message.builder()
                .content(dto.getContent())
                .isRead(false)
                .deletedBySender(false)
                .deletedByRecipient(false)
                .isEdited(false)
                .deliveryStatus(com.example.cleopatra.enums.DeliveryStatus.SENT)
                .build();
    }

    /**
     * Преобразование Page<Message> -> MessageListDto
     */
    public MessageListDto toMessageListDto(Page<Message> messagePage, User otherUser, Long currentUserId) {
        if (messagePage == null) {
            return MessageListDto.builder()
                    .messages(List.of())
                    .currentPage(0)
                    .totalPages(0)
                    .totalElements(0L)
                    .size(0)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }

        List<MessageResponseDto> messages = messagePage.getContent().stream()
                .map(message -> toResponseDto(message, currentUserId))
                .collect(Collectors.toList());

        return MessageListDto.builder()
                .messages(messages)
                .currentPage(messagePage.getNumber())
                .totalPages(messagePage.getTotalPages())
                .totalElements(messagePage.getTotalElements())
                .size(messagePage.getSize())
                .hasNext(messagePage.hasNext())
                .hasPrevious(messagePage.hasPrevious())
                .nextPage(messagePage.hasNext() ? messagePage.getNumber() + 1 : null)
                .previousPage(messagePage.hasPrevious() ? messagePage.getNumber() - 1 : null)
                .otherUser(toUserBriefDto(otherUser))
                .totalUnreadCount(0) // Будет заполнено в сервисе
                .build();
    }

    /**
     * Преобразование списка Message -> List<MessageResponseDto>
     */
    public List<MessageResponseDto> toResponseDtoList(List<Message> messages, Long currentUserId) {
        if (messages == null) {
            return List.of();
        }

        return messages.stream()
                .map(message -> toResponseDto(message, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Создание ConversationDto
     */
    public ConversationDto toConversationDto(User otherUser, Message lastMessage,
                                             Integer unreadCount, LocalDateTime lastActivity,
                                             Boolean isBlocked, Long currentUserId) {
        return ConversationDto.builder()
                .otherUser(toUserBriefDto(otherUser))
                .lastMessage(lastMessage != null ? toResponseDto(lastMessage, currentUserId) : null)
                .unreadCount(unreadCount != null ? unreadCount : 0)
                .lastActivity(lastActivity)
                .isBlocked(isBlocked != null ? isBlocked : false)
                .isOtherUserOnline(false) // Будет заполнено отдельно
                .otherUserStatus("") // Будет заполнено отдельно
                .build();
    }

    /**
     * Преобразование списка конверсаций в ConversationListDto
     */
    public ConversationListDto toConversationListDto(Page<ConversationDto> conversationPage,
                                                     Integer totalUnreadMessages) {
        if (conversationPage == null) {
            return ConversationListDto.builder()
                    .conversations(List.of())
                    .currentPage(0)
                    .totalPages(0)
                    .totalElements(0L)
                    .size(0)
                    .hasNext(false)
                    .hasPrevious(false)
                    .totalUnreadMessages(0)
                    .totalConversations(0)
                    .build();
        }

        return ConversationListDto.builder()
                .conversations(conversationPage.getContent())
                .currentPage(conversationPage.getNumber())
                .totalPages(conversationPage.getTotalPages())
                .totalElements(conversationPage.getTotalElements())
                .size(conversationPage.getSize())
                .hasNext(conversationPage.hasNext())
                .hasPrevious(conversationPage.hasPrevious())
                .totalUnreadMessages(totalUnreadMessages != null ? totalUnreadMessages : 0)
                .totalConversations((int) conversationPage.getTotalElements())
                .build();
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    /**
     * Обрезание текста до 50 символов
     */
    private String truncateContent(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    /**
     * Создание полного имени
     */
    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    /**
     * Обогащение MessageResponseDto дополнительной информацией
     */
    private void enrichMessageDto(MessageResponseDto dto, Long currentUserId) {
        if (dto == null || currentUserId == null) {
            return;
        }

        // Проверяем, является ли сообщение собственным
        dto.setIsOwnMessage(dto.getSender().getId().equals(currentUserId));

        // Права на редактирование (только свои сообщения в течение 15 минут)
        boolean canEdit = dto.getIsOwnMessage() &&
                dto.getCreatedAt() != null &&
                dto.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(15)) &&
                (dto.getIsEdited() == null || !dto.getIsEdited());
        dto.setCanEdit(canEdit);

        // Права на удаление (только свои сообщения)
        dto.setCanDelete(dto.getIsOwnMessage());

        // Время относительно текущего
        dto.setTimeAgo(getTimeAgo(dto.getCreatedAt()));
    }

    /**
     * Преобразование времени в читаемый формат "X минут назад"
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        try {
            Duration duration = Duration.between(dateTime, LocalDateTime.now());

            if (duration.toMinutes() < 1) {
                return "только что";
            } else if (duration.toMinutes() < 60) {
                return duration.toMinutes() + " мин. назад";
            } else if (duration.toHours() < 24) {
                return duration.toHours() + " ч. назад";
            } else if (duration.toDays() < 7) {
                return duration.toDays() + " дн. назад";
            } else {
                return dateTime.toLocalDate().toString();
            }
        } catch (Exception e) {
            log.warn("Ошибка при вычислении времени: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Установка информации об онлайн статусе пользователя
     */
    public void enrichUserBriefDto(UserBriefDto dto, boolean isOnline, String lastSeenText) {
        if (dto != null) {
            dto.setIsOnline(isOnline);
            dto.setLastSeenText(lastSeenText != null ? lastSeenText : "");
        }
    }

    /**
     * Установка счетчика непрочитанных сообщений
     */
    public void enrichMessageListDto(MessageListDto dto, Integer totalUnreadCount) {
        if (dto != null) {
            dto.setTotalUnreadCount(totalUnreadCount != null ? totalUnreadCount : 0);
        }
    }

    /**
     * Обновление содержимого сообщения (для редактирования)
     */
    public void updateMessageContent(Message message, String newContent) {
        if (message != null && newContent != null) {
            message.setContent(newContent);
            message.setIsEdited(true);
            message.setUpdatedAt(LocalDateTime.now());
        }
    }

    /**
     * Пометка сообщения как прочитанного
     */
    public void markMessageAsRead(Message message) {
        if (message != null && (message.getIsRead() == null || !message.getIsRead())) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            message.setDeliveryStatus(com.example.cleopatra.enums.DeliveryStatus.READ);
        }
    }

    /**
     * Пометка сообщения как удаленного для отправителя
     */
    public void markAsDeletedBySender(Message message) {
        if (message != null) {
            message.setDeletedBySender(true);
        }
    }

    /**
     * Пометка сообщения как удаленного для получателя
     */
    public void markAsDeletedByRecipient(Message message) {
        if (message != null) {
            message.setDeletedByRecipient(true);
        }
    }

    /**
     * Проверка, видимо ли сообщение для пользователя
     */
    public boolean isMessageVisibleToUser(Message message, Long userId) {
        if (message == null || userId == null) {
            return false;
        }

        // Проверяем, не удалено ли сообщение для этого пользователя
        if (message.getSender().getId().equals(userId)) {
            return message.getDeletedBySender() == null || !message.getDeletedBySender();
        } else if (message.getRecipient().getId().equals(userId)) {
            return message.getDeletedByRecipient() == null || !message.getDeletedByRecipient();
        }

        return false; // Пользователь не участвует в переписке
    }
}