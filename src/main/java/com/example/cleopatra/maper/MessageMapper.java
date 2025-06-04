package com.example.cleopatra.maper;

import com.example.cleopatra.dto.ChatMessage.*;
import com.example.cleopatra.model.Message;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageMapper {

    private final UserService userService;

    /**
     * Конвертация Message в MessageResponseDto
     */
    public MessageResponseDto toMessageResponseDto(Message message, User currentUser) {
        if (message == null) {
            return null;
        }

        UserBriefDto senderDto = userService.convertToUserBriefDto(message.getSender());
        UserBriefDto recipientDto = userService.convertToUserBriefDto(message.getRecipient());

        MessageBriefDto replyToDto = null;
        if (message.getReplyToMessage() != null) {
            replyToDto = toMessageBriefDto(message.getReplyToMessage());
        }

        boolean isOwnMessage = message.getSender().getId().equals(currentUser.getId());

        return MessageResponseDto.builder()
                .id(message.getId())
                .sender(senderDto)
                .recipient(recipientDto)
                .content(message.getContent())
                .isRead(message.getIsRead())
                .isEdited(message.getIsEdited())
                .deletedBySender(message.getDeletedBySender())
                .deletedByRecipient(message.getDeletedByRecipient())
                .readAt(message.getReadAt())
                .replyToMessage(replyToDto)
                .deliveryStatus(message.getDeliveryStatus())
                .deliveryStatusText(message.getDeliveryStatus().getDisplayName())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .isOwnMessage(isOwnMessage)
                .canEdit(canEditMessage(message, currentUser))
                .canDelete(canDeleteMessage(message, currentUser))
                .timeAgo(getTimeAgo(message.getCreatedAt()))
                .build();
    }

    /**
     * Конвертация Message в MessageBriefDto (для ответов на сообщения)
     */
    public MessageBriefDto toMessageBriefDto(Message message) {
        if (message == null) {
            return null;
        }

        UserBriefDto senderDto = userService.convertToUserBriefDto(message.getSender());

        return MessageBriefDto.builder()
                .id(message.getId())
                .sender(senderDto)
                .content(message.getContent())
                .shortContent(truncateContent(message.getContent(), 50))
                .build();
    }

    /**
     * Конвертация Message в ConversationDto
     */
    public ConversationDto toConversationDto(Message lastMessage, User currentUser) {
        if (lastMessage == null) {
            return null;
        }

        // Определяем, кто является собеседником
        User otherUser = lastMessage.getSender().getId().equals(currentUser.getId())
                ? lastMessage.getRecipient()
                : lastMessage.getSender();

        UserBriefDto otherUserDto = userService.convertToUserBriefDto(otherUser);
        MessageResponseDto lastMessageDto = toMessageResponseDto(lastMessage, currentUser);

        return ConversationDto.builder()
                .otherUser(otherUserDto)
                .lastMessage(lastMessageDto)
                .lastActivity(lastMessage.getCreatedAt())
                .isBlocked(false) // TODO: реализовать логику блокировки
                .isOtherUserOnline(otherUserDto.getIsOnline())
                .otherUserStatus(otherUserDto.getLastSeenText())
                .build();
    }

    /**
     * Конвертация Message в ConversationDto с указанием количества непрочитанных
     */
    public ConversationDto toConversationDto(Message lastMessage, User currentUser, int unreadCount) {
        ConversationDto conversationDto = toConversationDto(lastMessage, currentUser);
        if (conversationDto != null) {
            conversationDto.setUnreadCount(unreadCount);
        }
        return conversationDto;
    }

    /**
     * Конвертация Message в ChatMessage для WebSocket
     */
    public ChatMessage toChatMessage(Message message, String action) {
        if (message == null) {
            return null;
        }

        return ChatMessage.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .recipientId(message.getRecipient().getId())
                .content(message.getContent())
                .type(message.getMessageType())
                .timestamp(message.getCreatedAt())
                .isRead(message.getIsRead())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .action(action)
                .build();
    }

    /**
     * Создание ChatMessage для уведомлений
     */
    public ChatMessage createNotificationMessage(Long senderId, Long recipientId, String action) {
        return ChatMessage.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .action(action)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Создание ChatMessage для уведомления о прочтении
     */
    public ChatMessage createReadNotification(Long senderId, Long recipientId) {
        return createNotificationMessage(senderId, recipientId, "MESSAGES_READ");
    }

    /**
     * Создание ChatMessage для уведомления о редактировании
     */
    public ChatMessage createEditNotification(Message message) {
        return ChatMessage.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .recipientId(message.getRecipient().getId())
                .content(message.getContent())
                .action("MESSAGE_EDITED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Создание ChatMessage for удаления сообщения
     */
    public ChatMessage createDeleteNotification(Long messageId, Long senderId, Long recipientId) {
        return ChatMessage.builder()
                .id(messageId)
                .senderId(senderId)
                .recipientId(recipientId)
                .action("MESSAGE_DELETED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Создание ChatMessage для уведомления о наборе текста
     */
    public ChatMessage createTypingNotification(Long senderId, Long recipientId, boolean isTyping) {
        return ChatMessage.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .action(isTyping ? "TYPING_START" : "TYPING_STOP")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Конвертация списка сообщений
     */
    public List<MessageResponseDto> toMessageResponseDtoList(List<Message> messages, User currentUser) {
        return messages.stream()
                .map(message -> toMessageResponseDto(message, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * Конвертация списка сообщений в ConversationDto
     */
    public List<ConversationDto> toConversationDtoList(List<Message> lastMessages, User currentUser) {
        return lastMessages.stream()
                .map(message -> toConversationDto(message, currentUser))
                .collect(Collectors.toList());
    }

    // Вспомогательные методы

    /**
     * Проверка возможности редактирования сообщения
     */
    private boolean canEditMessage(Message message, User currentUser) {
        if (!message.getSender().getId().equals(currentUser.getId())) {
            return false;
        }
        // Можно редактировать в течение 24 часов
        return message.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * Проверка возможности удаления сообщения
     */
    private boolean canDeleteMessage(Message message, User currentUser) {
        return message.getSender().getId().equals(currentUser.getId()) ||
                message.getRecipient().getId().equals(currentUser.getId());
    }

    /**
     * Обрезка содержимого сообщения
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * Получение строки "время назад"
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutes < 1) return "только что";
        if (minutes < 60) return minutes + " мин. назад";

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + " ч. назад";

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) return days + " дн. назад";

        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    /**
     * Создание StatusMessage для WebSocket
     */
    public StatusMessage createStatusMessage(Long userId, String status) {
        return StatusMessage.builder()
                .userId(userId)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Конвертация User в UserContactDto
     */
    public UserContactDto toUserContactDto(User user, User currentUser, boolean hasUnreadMessages, int unreadCount) {
        if (user == null) {
            return null;
        }

        return UserContactDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .imageUrl(user.getImageUrl())
//                .isOnline(user.getIsOnline())
//                .lastSeenText(getLastSeenText(user.getLastSeen()))
                .isFriend(true) // TODO: реализовать логику дружбы
                .isFollowing(false) // TODO: реализовать логику подписок
                .isFollower(false) // TODO: реализовать логику подписок
                .mutualFriendsCount(0) // TODO: реализовать подсчет взаимных друзей
                .lastInteraction(LocalDateTime.now()) // TODO: получать реальное время последнего взаимодействия
                .hasUnreadMessages(hasUnreadMessages)
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * Получение текста последнего посещения
     */
    private String getLastSeenText(LocalDateTime lastSeen) {
        if (lastSeen == null) {
            return "недавно";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(lastSeen, now);

        if (minutes < 5) return "в сети";
        if (minutes < 60) return minutes + " мин. назад";

        long hours = ChronoUnit.HOURS.between(lastSeen, now);
        if (hours < 24) return hours + " ч. назад";

        long days = ChronoUnit.DAYS.between(lastSeen, now);
        if (days < 7) return days + " дн. назад";

        return lastSeen.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}