//package com.example.cleopatra.service;
//import com.example.cleopatra.config.ChatWebSocketHandler;
//import com.example.cleopatra.dto.ChatMessage.*;
//import com.example.cleopatra.enums.DeliveryStatus;
//import com.example.cleopatra.enums.MessageType;
//import com.example.cleopatra.maper.MessageMapper;
//import com.example.cleopatra.model.Message;
//import com.example.cleopatra.model.User;
//import com.example.cleopatra.repository.MessageRepository;
//import com.example.cleopatra.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//@Transactional
//public class MessageService {
//
//    private final MessageRepository messageRepository;
//    private final UserRepository userRepository;
//    private final ChatWebSocketHandler webSocketHandler;
//    private final UserService userService;
//    private final MessageMapper messageMapper ;
//
//    /**
//     * Отправить новое сообщение
//     */
//    public MessageResponseDto sendMessage(MessageCreateDto createDto) {
//        // Получаем текущего пользователя
//        User sender = getCurrentUser();
//
//        // Получаем получателя
//        User recipient = userRepository.findById(createDto.getRecipientId())
//                .orElseThrow(() -> new RuntimeException("Получатель не найден"));
//
//        // Проверяем права на отправку сообщения
//        validateCanSendMessage(sender, recipient);
//
//        // Создаем сообщение
//        Message message = Message.builder()
//                .sender(sender)
//                .recipient(recipient)
//                .content(createDto.getContent())
//                .messageType(MessageType.TEXT)
//                .isRead(false)
//                .isEdited(false)
//                .deletedBySender(false)
//                .deletedByRecipient(false)
//                .deliveryStatus(DeliveryStatus.SENT)
//                .build();
//
//        // Если это ответ на сообщение
//        if (createDto.getReplyToMessageId() != null) {
//            Message replyToMessage = messageRepository.findById(createDto.getReplyToMessageId())
//                    .orElseThrow(() -> new RuntimeException("Сообщение для ответа не найдено"));
//            message.setReplyToMessage(replyToMessage);
//        }
//
//        // Сохраняем сообщение
//        message = messageRepository.save(message);
//
//        // Конвертируем в DTO
//        MessageResponseDto responseDto = convertToResponseDto(message, sender);
//
//        // Отправляем через WebSocket
//        sendWebSocketMessage(message, responseDto);
//
//        // Обновляем статус доставки
//        updateDeliveryStatus(message.getId(), DeliveryStatus.DELIVERED);
//
//        log.info("Message sent from {} to {}", sender.getId(), recipient.getId());
//        return responseDto;
//    }
//
//    /**
//     * Получить список сообщений в конверсации
//     */
//    @Transactional(readOnly = true)
//    public MessageListDto getConversation(Long otherUserId, int page, int size) {
//        User currentUser = getCurrentUser();
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
//        Page<Message> messagesPage = messageRepository.findConversation(
//                currentUser.getId(), otherUserId, pageable);
//
//        List<MessageResponseDto> messages = messagesPage.getContent().stream()
//                .map(message -> convertToResponseDto(message, currentUser))
//                .collect(Collectors.toList());
//
//        // Получаем информацию о собеседнике
//        User otherUser = userRepository.findById(otherUserId)
//                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
//
//        UserBriefDto otherUserDto = userService.convertToUserBriefDto(otherUser);
//
//        // Подсчитываем непрочитанные сообщения
//        Long unreadCount = messageRepository.countUnreadMessages(currentUser.getId());
//
//        return MessageListDto.builder()
//                .messages(messages)
//                .currentPage(page)
//                .totalPages(messagesPage.getTotalPages())
//                .totalElements(messagesPage.getTotalElements())
//                .size(size)
//                .hasNext(messagesPage.hasNext())
//                .hasPrevious(messagesPage.hasPrevious())
//                .nextPage(messagesPage.hasNext() ? page + 1 : null)
//                .previousPage(messagesPage.hasPrevious() ? page - 1 : null)
//                .otherUser(otherUserDto)
//                .totalUnreadCount(unreadCount.intValue())
//                .build();
//    }
//
//    /**
//     * Отметить сообщения как прочитанные
//     */
//    public void markMessagesAsRead(Long senderId) {
//        User currentUser = getCurrentUser();
//
//        List<Message> unreadMessages = messageRepository.findUnreadMessagesFromSender(
//                senderId, currentUser.getId());
//
//        for (Message message : unreadMessages) {
//            message.setIsRead(true);
//            message.setReadAt(LocalDateTime.now());
//            message.setDeliveryStatus(DeliveryStatus.READ);
//        }
//
//        if (!unreadMessages.isEmpty()) {
//            messageRepository.saveAll(unreadMessages);
//
//            // Уведомляем отправителя о прочтении через WebSocket
//            ChatMessage readNotification = ChatMessage.builder()
//                    .action("MESSAGES_READ")
//                    .senderId(currentUser.getId())
//                    .recipientId(senderId)
//                    .timestamp(LocalDateTime.now())
//                    .build();
//
//            webSocketHandler.sendToUser(senderId, readNotification);
//
//            log.info("Marked {} messages as read", unreadMessages.size());
//        }
//    }
//
//    /**
//     * Редактировать сообщение
//     */
//    public MessageResponseDto editMessage(Long messageId, MessageEditDto editDto) {
//        User currentUser = getCurrentUser();
//
//        Message message = messageRepository.findById(messageId)
//                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
//
//        // Проверяем права на редактирование
//        if (!message.getSender().getId().equals(currentUser.getId())) {
//            throw new RuntimeException("Нет прав на редактирование этого сообщения");
//        }
//
//        // Проверяем время (можно редактировать только в течение 24 часов)
//        if (message.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
//            throw new RuntimeException("Сообщение можно редактировать только в течение 24 часов");
//        }
//
//        // Обновляем сообщение
//        message.setContent(editDto.getContent());
//        message.setIsEdited(true);
//        message = messageRepository.save(message);
//
//
//        MessageResponseDto responseDto = convertToResponseDto(message, currentUser);
//
//        // Уведомляем через WebSocket
//        ChatMessage editNotification = ChatMessage.builder()
//                .id(message.getId())
//                .senderId(currentUser.getId())
//                .recipientId(message.getRecipient().getId())
//                .content(message.getContent())
//                .action("MESSAGE_EDITED")
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        webSocketHandler.sendToUser(message.getRecipient().getId(), editNotification);
//
//        return responseDto;
//    }
//
//    /**
//     * Удалить сообщение
//     */
//    public void deleteMessage(Long messageId, boolean deleteForEveryone) {
//        User currentUser = getCurrentUser();
//
//        Message message = messageRepository.findById(messageId)
//                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
//
//        if (!message.getSender().getId().equals(currentUser.getId()) &&
//                !message.getRecipient().getId().equals(currentUser.getId())) {
//            throw new RuntimeException("Нет прав на удаление этого сообщения");
//        }
//
//        if (deleteForEveryone && !message.getSender().getId().equals(currentUser.getId())) {
//            throw new RuntimeException("Удалить для всех может только отправитель");
//        }
//
//        if (deleteForEveryone) {
//            // Удаляем сообщение полностью
//            messageRepository.delete(message);
//
//            // Уведомляем получателя
//            ChatMessage deleteNotification = ChatMessage.builder()
//                    .id(messageId)
//                    .senderId(currentUser.getId())
//                    .recipientId(message.getRecipient().getId())
//                    .action("MESSAGE_DELETED")
//                    .timestamp(LocalDateTime.now())
//                    .build();
//
//            webSocketHandler.sendToUser(message.getRecipient().getId(), deleteNotification);
//        } else {
//            // Помечаем как удаленное для текущего пользователя
//            if (message.getSender().getId().equals(currentUser.getId())) {
//                message.setDeletedBySender(true);
//            } else {
//                message.setDeletedByRecipient(true);
//            }
//            messageRepository.save(message);
//        }
//
//        log.info("Message {} deleted by user {}", messageId, currentUser.getId());
//    }
//
//    /**
//     * Получить список конверсаций - ИСПРАВЛЕННАЯ ВЕРСИЯ
//     */
//    @Transactional(readOnly = true)
//    public ConversationListDto getConversations(int page, int size) {
//        User currentUser = getCurrentUser();
//
//        // Получаем последние сообщения для каждой конверсации
//        Page<Message> lastMessagesPage = messageRepository.findUserConversations(
//                currentUser.getId(), PageRequest.of(page, size));
//
//        List<ConversationDto> conversations = lastMessagesPage.getContent().stream()
//                .map(message -> convertMessageToConversationDto(message, currentUser))
//                .collect(Collectors.toList());
//
//        // Подсчитываем общее количество непрочитанных сообщений
//        Long totalUnreadMessages = messageRepository.countUnreadMessages(currentUser.getId());
//
//        return ConversationListDto.builder()
//                .conversations(conversations)
//                .currentPage(page)
//                .totalPages(lastMessagesPage.getTotalPages())
//                .totalElements(lastMessagesPage.getTotalElements())
//                .size(size)
//                .hasNext(lastMessagesPage.hasNext())
//                .hasPrevious(lastMessagesPage.hasPrevious())
//                .totalUnreadMessages(totalUnreadMessages.intValue())
//                .totalConversations(conversations.size())
//                .build();
//    }
//
//    /**
//     * НОВЫЙ метод для конвертации Message в ConversationDto
//     */
//    private ConversationDto convertMessageToConversationDto(Message lastMessage, User currentUser) {
//        // Определяем, кто является собеседником
//        User otherUser = lastMessage.getSender().getId().equals(currentUser.getId())
//                ? lastMessage.getRecipient()
//                : lastMessage.getSender();
//
//        UserBriefDto otherUserDto = userService.convertToUserBriefDto(otherUser);
//        MessageResponseDto lastMessageDto = convertToResponseDto(lastMessage, currentUser);
//
//        // Подсчитываем непрочитанные сообщения от этого пользователя
//        List<Message> unreadMessages = messageRepository.findUnreadMessagesFromSender(
//                otherUser.getId(), currentUser.getId());
//
//        return ConversationDto.builder()
//                .otherUser(otherUserDto)
//                .lastMessage(lastMessageDto)
//                .unreadCount(unreadMessages.size())
//                .lastActivity(lastMessage.getCreatedAt())
//                .isBlocked(false) // TODO: реализовать логику блокировки
//                .isOtherUserOnline(otherUserDto.getIsOnline())
//                .otherUserStatus(otherUserDto.getLastSeenText())
//                .build();
//    }
//
//
////    /**
////     * Альтернативная реализация getConversations с более простой логикой
////     */
////    @Transactional(readOnly = true)
////    public ConversationListDto getConversationsAlternative(int page, int size) {
////        User currentUser = getCurrentUser();
////
////        // Получаем всех пользователей, с которыми есть переписка
////        List<Long> conversationUserIds = getConversationUserIds(currentUser.getId());
////
////        // Для каждого пользователя получаем последнее сообщение
////        List<ConversationDto> conversations = conversationUserIds.stream()
////                .skip((long) page * size)
////                .limit(size)
////                .map(userId -> {
////                    // Получаем последнее сообщение с этим пользователем
////                    Page<Message> lastMessagePage = messageRepository.findConversation(
////                            currentUser.getId(), userId, PageRequest.of(0, 1));
////
////                    if (lastMessagePage.hasContent()) {
////                        Message lastMessage = lastMessagePage.getContent().get(0);
////                        return convertMessageToConversationDto(lastMessage, currentUser);
////                    }
////                    return null;
////                })
////                .filter(Objects::nonNull)
////                .collect(Collectors.toList());
////
////        Long totalUnreadMessages = messageRepository.countUnreadMessages(currentUser.getId());
////
////        return ConversationListDto.builder()
////                .conversations(conversations)
////                .currentPage(page)
////                .totalPages((conversationUserIds.size() + size - 1) / size)
////                .totalElements((long) conversationUserIds.size())
////                .size(size)
////                .hasNext((page + 1) * size < conversationUserIds.size())
////                .hasPrevious(page > 0)
////                .totalUnreadMessages(totalUnreadMessages.intValue())
////                .totalConversations(conversationUserIds.size())
////                .build();
////    }
////
////    /**
////     * Получить ID всех пользователей, с которыми есть переписка
////     */
////    private List<Long> getConversationUserIds(Long currentUserId) {
////        List<Message> allUserMessages = messageRepository.findAll().stream()
////                .filter(m -> m.getSender().getId().equals(currentUserId) || m.getRecipient().getId().equals(currentUserId))
////                .filter(m -> (m.getSender().getId().equals(currentUserId) && !m.getDeletedBySender()) ||
////                        (m.getRecipient().getId().equals(currentUserId) && !m.getDeletedByRecipient()))
////                .collect(Collectors.toList());
////
////        return allUserMessages.stream()
////                .map(m -> m.getSender().getId().equals(currentUserId) ? m.getRecipient().getId() : m.getSender().getId())
////                .distinct()
////                .collect(Collectors.toList());
////    }
//
//    /**
//     * Поиск сообщений
//     */
//    @Transactional(readOnly = true)
//    public MessageListDto searchMessages(MessageSearchDto searchDto) {
//        User currentUser = getCurrentUser();
//
//        Pageable pageable = PageRequest.of(
//                searchDto.getPage(),
//                searchDto.getSize(),
//                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
//        );
//
//        Page<Message> messagesPage = messageRepository.searchMessages(
//                currentUser.getId(),
//                searchDto.getSearchText(),
//                searchDto.getOtherUserId(),
//                searchDto.getDateFrom(),
//                searchDto.getDateTo(),
//                searchDto.getUnreadOnly(),
//                pageable
//        );
//
//        List<MessageResponseDto> messages = messagesPage.getContent().stream()
//                .map(message -> convertToResponseDto(message, currentUser))
//                .collect(Collectors.toList());
//
//        return MessageListDto.builder()
//                .messages(messages)
//                .currentPage(searchDto.getPage())
//                .totalPages(messagesPage.getTotalPages())
//                .totalElements(messagesPage.getTotalElements())
//                .size(searchDto.getSize())
//                .hasNext(messagesPage.hasNext())
//                .hasPrevious(messagesPage.hasPrevious())
//                .build();
//    }
//
//    /**
//     * Отправить уведомление о наборе текста
//     */
//    public void sendTypingNotification(Long recipientId, boolean isTyping) {
//        User currentUser = getCurrentUser();
//
//        ChatMessage typingMessage = ChatMessage.builder()
//                .senderId(currentUser.getId())
//                .recipientId(recipientId)
//                .action(isTyping ? "TYPING_START" : "TYPING_STOP")
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        webSocketHandler.sendToUser(recipientId, typingMessage);
//    }
//
//    // Приватные методы
//
//    private User getCurrentUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//        return userRepository.findByEmail(username)
//                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
//    }
//
//    private void validateCanSendMessage(User sender, User recipient) {
//        // Здесь добавить логику проверки:
//        // - не заблокирован ли отправитель получателем
//        // - не является ли профиль получателя приватным
//        // - есть ли права на отправку сообщений
//
//        // Пример базовой проверки
//        if (sender.getId().equals(recipient.getId())) {
//            throw new RuntimeException("Нельзя отправить сообщение самому себе");
//        }
//    }
//
//    private MessageResponseDto convertToResponseDto(Message message, User currentUser) {
//        UserBriefDto senderDto = userService.convertToUserBriefDto(message.getSender());
//        UserBriefDto recipientDto = userService.convertToUserBriefDto(message.getRecipient());
//
//        MessageBriefDto replyToDto = null;
//        if (message.getReplyToMessage() != null) {
//            Message replyTo = message.getReplyToMessage();
//            replyToDto = MessageBriefDto.builder()
//                    .id(replyTo.getId())
//                    .sender(userService.convertToUserBriefDto(replyTo.getSender()))
//                    .content(replyTo.getContent())
//                    .shortContent(truncateContent(replyTo.getContent(), 50))
//                    .build();
//        }
//
//        boolean isOwnMessage = message.getSender().getId().equals(currentUser.getId());
//
//        return MessageResponseDto.builder()
//                .id(message.getId())
//                .sender(senderDto)
//                .recipient(recipientDto)
//                .content(message.getContent())
//                .isRead(message.getIsRead())
//                .isEdited(message.getIsEdited())
//                .deletedBySender(message.getDeletedBySender())
//                .deletedByRecipient(message.getDeletedByRecipient())
//                .readAt(message.getReadAt())
//                .replyToMessage(replyToDto)
//                .deliveryStatus(message.getDeliveryStatus())
//                .deliveryStatusText(message.getDeliveryStatus().getDisplayName())
//                .createdAt(message.getCreatedAt())
//                .updatedAt(message.getUpdatedAt())
//                .isOwnMessage(isOwnMessage)
//                .canEdit(canEditMessage(message, currentUser))
//                .canDelete(canDeleteMessage(message, currentUser))
//                .timeAgo(getTimeAgo(message.getCreatedAt()))
//                .build();
//    }
//
//    private ConversationDto convertToConversationDto(Object[] result) {
//        // Результат запроса содержит: otherUser, lastMessage, unreadCount, lastActivity
//        User otherUser = (User) result[0];
//        Message lastMessage = (Message) result[1];
//        Long unreadCount = (Long) result[2];
//        LocalDateTime lastActivity = (LocalDateTime) result[3];
//
//        UserBriefDto otherUserDto = userService.convertToUserBriefDto(otherUser);
//        MessageResponseDto lastMessageDto = null;
//
//        if (lastMessage != null) {
//            lastMessageDto = convertToResponseDto(lastMessage, getCurrentUser());
//        }
//
//        return ConversationDto.builder()
//                .otherUser(otherUserDto)
//                .lastMessage(lastMessageDto)
//                .unreadCount(unreadCount.intValue())
//                .lastActivity(lastActivity)
//                .isBlocked(false) // TODO: реализовать логику блокировки
//                .isOtherUserOnline(otherUserDto.getIsOnline())
//                .otherUserStatus(otherUserDto.getLastSeenText())
//                .build();
//    }
//
//    private void sendWebSocketMessage(Message message, MessageResponseDto responseDto) {
//        ChatMessage chatMessage = ChatMessage.builder()
//                .id(message.getId())
//                .senderId(message.getSender().getId())
//                .recipientId(message.getRecipient().getId())
//                .content(message.getContent())
//                .type(message.getMessageType())
//                .timestamp(message.getCreatedAt())
//                .isRead(message.getIsRead())
//                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
//                .action("NEW_MESSAGE")
//                .build();
//
//        webSocketHandler.sendToUser(message.getRecipient().getId(), chatMessage);
//    }
//
//
//    /**
//     * Получить количество непрочитанных сообщений
//     */
//    @Transactional(readOnly = true)
//    public Long getUnreadMessagesCount() {
//        User currentUser = getCurrentUser();
//        return messageRepository.countUnreadMessages(currentUser.getId());
//    }
//
//    /**
//     * Получить статистику сообщений
//     */
//    @Transactional(readOnly = true)
//    public MessageStatisticsDto getMessageStatistics() {
//        User currentUser = getCurrentUser();
//
//        Long totalSent = messageRepository.countSentMessages(currentUser.getId());
//        Long totalReceived = messageRepository.countReceivedMessages(currentUser.getId());
//        Long unreadCount = messageRepository.countUnreadMessages(currentUser.getId());
//        Long totalConversations = messageRepository.countConversations(currentUser.getId());
//
//        return MessageStatisticsDto.builder()
//                .totalSentMessages(totalSent)
//                .totalReceivedMessages(totalReceived)
//                .unreadMessagesCount(unreadCount)
//                .totalConversations(totalConversations)
//                .build();
//    }
//
//    private void updateDeliveryStatus(Long messageId, DeliveryStatus status) {
//        // Асинхронно обновляем статус доставки
//        messageRepository.findById(messageId).ifPresent(message -> {
//            message.setDeliveryStatus(status);
//            messageRepository.save(message);
//        });
//    }
//
//    private boolean canEditMessage(Message message, User currentUser) {
//        if (!message.getSender().getId().equals(currentUser.getId())) {
//            return false;
//        }
//        // Можно редактировать в течение 24 часов
//        return message.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24));
//    }
//
//    private boolean canDeleteMessage(Message message, User currentUser) {
//        return message.getSender().getId().equals(currentUser.getId()) ||
//                message.getRecipient().getId().equals(currentUser.getId());
//    }
//
//    private String truncateContent(String content, int maxLength) {
//        if (content == null || content.length() <= maxLength) {
//            return content;
//        }
//        return content.substring(0, maxLength) + "...";
//    }
//
//    private String getTimeAgo(LocalDateTime dateTime) {
//        if (dateTime == null) return "";
//
//        LocalDateTime now = LocalDateTime.now();
//        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
//
//        if (minutes < 1) return "только что";
//        if (minutes < 60) return minutes + " мин. назад";
//
//        long hours = ChronoUnit.HOURS.between(dateTime, now);
//        if (hours < 24) return hours + " ч. назад";
//
//        long days = ChronoUnit.DAYS.between(dateTime, now);
//        if (days < 7) return days + " дн. назад";
//
//        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
//    }
//
//    @Transactional(readOnly = true)
//    public Long getTotalUnreadCount(Long userId) {
//        return messageRepository.countUnreadMessages(userId);
//    }
//
//    @Transactional(readOnly = true)
//    public ConversationListDto getUserConversations(Long userId, int page, int size) {
//        // Используем уже существующий метод getConversations()
//        return getConversations(page, size);
//    }
//}

package com.example.cleopatra.service;

import com.example.cleopatra.config.ChatWebSocketHandler;
import com.example.cleopatra.dto.ChatMessage.*;
import com.example.cleopatra.enums.DeliveryStatus;
import com.example.cleopatra.enums.MessageType;
import com.example.cleopatra.maper.MessageMapper;
import com.example.cleopatra.model.Message;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.MessageRepository;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatWebSocketHandler webSocketHandler;
    private final UserService userService;
    private final MessageMapper messageMapper;


    /**
     * Отправить новое сообщение
     */
    public MessageResponseDto sendMessage(MessageCreateDto createDto) {
        // Получаем текущего пользователя
        User sender = getCurrentUser();

        // Получаем получателя
        User recipient = userRepository.findById(createDto.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        // Проверяем права на отправку сообщения
        validateCanSendMessage(sender, recipient);

        // Создаем сообщение
        Message message = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .content(createDto.getContent())
                .messageType(MessageType.TEXT)
                .isRead(false)
                .isEdited(false)
                .deletedBySender(false)
                .deletedByRecipient(false)
                .deliveryStatus(DeliveryStatus.SENT)
                .build();

        // Если это ответ на сообщение
        if (createDto.getReplyToMessageId() != null) {
            Message replyToMessage = messageRepository.findById(createDto.getReplyToMessageId())
                    .orElseThrow(() -> new RuntimeException("Сообщение для ответа не найдено"));
            message.setReplyToMessage(replyToMessage);
        }

        // Сохраняем сообщение
        message = messageRepository.save(message);
        // Конвертируем в DTO используя маппер
        MessageResponseDto responseDto = messageMapper.toMessageResponseDto(message, sender);

        // Отправляем через WebSocket
        ChatMessage chatMessage = messageMapper.createNewMessageNotification(message);
        webSocketHandler.sendToUser(message.getRecipient().getId(), chatMessage);

        // Обновляем статус доставки
        updateDeliveryStatus(message.getId(), DeliveryStatus.DELIVERED);

        afterMessageSent(message.getRecipient().getId());

        log.info("Message sent from {} to {}", sender.getId(), recipient.getId());
        return responseDto;
    }


    /**
     * Получить список сообщений в конверсации с правильной пагинацией
     */
    @Transactional(readOnly = true)
    public MessageListDto getConversation(Long otherUserId, int page, int size) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagesPage;

        // ИЗМЕНИТЬ ЛОГИКУ: всегда загружаем последние сообщения для первой страницы
        messagesPage = messageRepository.findConversationLatest(
                currentUser.getId(), otherUserId, pageable);

        // ВАЖНО: Разворачиваем список, чтобы старые сообщения были сверху
        List<Message> reversedMessages = new ArrayList<>(messagesPage.getContent());
        Collections.reverse(reversedMessages);

        List<MessageResponseDto> messages = messageMapper.toMessageResponseDtoList(
                reversedMessages, currentUser);

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserBriefDto otherUserDto = userService.convertToUserBriefDto(otherUser);
        Long unreadCount = messageRepository.countUnreadMessages(currentUser.getId());

        return MessageListDto.builder()
                .messages(messages)
                .currentPage(page)
                .totalPages(messagesPage.getTotalPages())
                .totalElements(messagesPage.getTotalElements())
                .size(size)
                .hasNext(messagesPage.hasNext())
                .hasPrevious(messagesPage.hasPrevious())
                .nextPage(messagesPage.hasNext() ? page + 1 : null)
                .previousPage(messagesPage.hasPrevious() ? page - 1 : null)
                .otherUser(otherUserDto)
                .totalUnreadCount(unreadCount.intValue())
                .build();
    }

    /**
     * Загрузка более старых сообщений для "Load More"
     */
    @Transactional(readOnly = true)
    public MessageListDto loadMoreMessages(Long otherUserId, LocalDateTime beforeDate, int size) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(0, size);
        Page<Message> messagesPage = messageRepository.findConversationBefore(
                currentUser.getId(), otherUserId, beforeDate, pageable);

        // Разворачиваем для правильного порядка
        List<Message> reversedMessages = new ArrayList<>(messagesPage.getContent());
        Collections.reverse(reversedMessages);

        List<MessageResponseDto> messages = messageMapper.toMessageResponseDtoList(
                reversedMessages, currentUser);

        return MessageListDto.builder()
                .messages(messages)
                .currentPage(0)
                .totalPages(messagesPage.getTotalPages())
                .totalElements(messagesPage.getTotalElements())
                .size(size)
                .hasNext(messagesPage.hasNext())
                .hasPrevious(false)
                .nextPage(messagesPage.hasNext() ? 1 : null)
                .previousPage(null)
                .build();
    }

    /**
     * Загрузка более старых сообщений для "Load More" по timestamp
     */
    @Transactional(readOnly = true)
    public MessageListDto loadMoreMessagesByTimestamp(Long otherUserId, String beforeTimestamp, int size) {
        User currentUser = getCurrentUser();

        try {
            // Парсим timestamp
            LocalDateTime beforeDate = LocalDateTime.parse(beforeTimestamp);

            // Используем существующий метод
            return loadMoreMessages(otherUserId, beforeDate, size);

        } catch (Exception e) {
            log.error("Error parsing timestamp: {}", beforeTimestamp, e);
            throw new RuntimeException("Неверный формат timestamp");
        }
    }

    /**
     * Отметить сообщения как прочитанные
     */
    public void markMessagesAsRead(Long senderId) {
        User currentUser = getCurrentUser();

        List<Message> unreadMessages = messageRepository.findUnreadMessagesFromSender(
                senderId, currentUser.getId());

        for (Message message : unreadMessages) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            message.setDeliveryStatus(DeliveryStatus.READ);
        }

        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);

            // Уведомляем отправителя о прочтении через WebSocket используя маппер
            ChatMessage readNotification = messageMapper.createReadNotification(
                    currentUser.getId(), senderId);
            webSocketHandler.sendToUser(senderId, readNotification);

            log.info("Marked {} messages as read", unreadMessages.size());
        }
    }

    /**
     * Редактировать сообщение
     */
    public MessageResponseDto editMessage(Long messageId, MessageEditDto editDto) {
        User currentUser = getCurrentUser();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        // Проверяем права на редактирование
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Нет прав на редактирование этого сообщения");
        }

        // Проверяем время (можно редактировать только в течение 24 часов)
        if (message.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new RuntimeException("Сообщение можно редактировать только в течение 24 часов");
        }

        // Обновляем сообщение
        message.setContent(editDto.getContent());
        message.setIsEdited(true);
        message = messageRepository.save(message);

        // Конвертируем в DTO используя маппер
        MessageResponseDto responseDto = messageMapper.toMessageResponseDto(message, currentUser);

        // Уведомляем через WebSocket используя маппер
        ChatMessage editNotification = messageMapper.createEditNotification(message);
        webSocketHandler.sendToUser(message.getRecipient().getId(), editNotification);

        return responseDto;
    }

    /**
     * Удалить сообщение
     */
    public void deleteMessage(Long messageId, boolean deleteForEveryone) {
        User currentUser = getCurrentUser();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        if (!message.getSender().getId().equals(currentUser.getId()) &&
                !message.getRecipient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Нет прав на удаление этого сообщения");
        }

        if (deleteForEveryone && !message.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Удалить для всех может только отправитель");
        }

        if (deleteForEveryone) {
            // Удаляем сообщение полностью
            messageRepository.delete(message);

            // Уведомляем получателя используя маппер
            ChatMessage deleteNotification = messageMapper.createDeleteNotification(
                    messageId, currentUser.getId(), message.getRecipient().getId());
            webSocketHandler.sendToUser(message.getRecipient().getId(), deleteNotification);
        } else {
            // Помечаем как удаленное для текущего пользователя
            if (message.getSender().getId().equals(currentUser.getId())) {
                message.setDeletedBySender(true);
            } else {
                message.setDeletedByRecipient(true);
            }
            messageRepository.save(message);
        }

        log.info("Message {} deleted by user {}", messageId, currentUser.getId());
    }

    /**
     * Получить список конверсаций
     */
    @Transactional(readOnly = true)
    public ConversationListDto getConversations(int page, int size) {
        User currentUser = getCurrentUser();

        // Получаем последние сообщения для каждой конверсации
        Page<Message> lastMessagesPage = messageRepository.findUserConversations(
                currentUser.getId(), PageRequest.of(page, size));

        List<ConversationDto> conversations = lastMessagesPage.getContent().stream()
                .map(message -> convertMessageToConversationDto(message, currentUser))
                .collect(Collectors.toList());

        // Подсчитываем общее количество непрочитанных сообщений
        Long totalUnreadMessages = messageRepository.countUnreadMessages(currentUser.getId());

        return ConversationListDto.builder()
                .conversations(conversations)
                .currentPage(page)
                .totalPages(lastMessagesPage.getTotalPages())
                .totalElements(lastMessagesPage.getTotalElements())
                .size(size)
                .hasNext(lastMessagesPage.hasNext())
                .hasPrevious(lastMessagesPage.hasPrevious())
                .totalUnreadMessages(totalUnreadMessages.intValue())
                .totalConversations(conversations.size())
                .build();
    }

    /**
     * Конвертация Message в ConversationDto используя маппер
     */
    private ConversationDto convertMessageToConversationDto(Message lastMessage, User currentUser) {
        // Определяем, кто является собеседником
        User otherUser = lastMessage.getSender().getId().equals(currentUser.getId())
                ? lastMessage.getRecipient()
                : lastMessage.getSender();

        // Подсчитываем непрочитанные сообщения от этого пользователя
        List<Message> unreadMessages = messageRepository.findUnreadMessagesFromSender(
                otherUser.getId(), currentUser.getId());

        // Используем маппер для создания ConversationDto
        return messageMapper.toConversationDto(lastMessage, currentUser, unreadMessages.size());
    }

    /**
     * Поиск сообщений
     */
    @Transactional(readOnly = true)
    public MessageListDto searchMessages(MessageSearchDto searchDto) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        Page<Message> messagesPage = messageRepository.searchMessages(
                currentUser.getId(),
                searchDto.getSearchText(),
                searchDto.getOtherUserId(),
                searchDto.getDateFrom(),
                searchDto.getDateTo(),
                searchDto.getUnreadOnly(),
                pageable
        );

        // Используем маппер для конвертации списка сообщений
        List<MessageResponseDto> messages = messageMapper.toMessageResponseDtoList(
                messagesPage.getContent(), currentUser);

        return MessageListDto.builder()
                .messages(messages)
                .currentPage(searchDto.getPage())
                .totalPages(messagesPage.getTotalPages())
                .totalElements(messagesPage.getTotalElements())
                .size(searchDto.getSize())
                .hasNext(messagesPage.hasNext())
                .hasPrevious(messagesPage.hasPrevious())
                .build();
    }

    /**
     * Отправить уведомление о наборе текста
     */
    public void sendTypingNotification(Long recipientId, boolean isTyping) {
        User currentUser = getCurrentUser();

        // Используем маппер для создания уведомления о наборе текста
        ChatMessage typingMessage = messageMapper.createTypingNotification(
                currentUser.getId(), recipientId, isTyping);
        webSocketHandler.sendToUser(recipientId, typingMessage);
    }

    /**
     * Получить количество непрочитанных сообщений
     */
    @Transactional(readOnly = true)
    public Long getUnreadMessagesCount() {
        User currentUser = getCurrentUser();
        return messageRepository.countUnreadMessages(currentUser.getId());
    }

    /**
     * Получить статистику сообщений
     */
    @Transactional(readOnly = true)
    public MessageStatisticsDto getMessageStatistics() {
        User currentUser = getCurrentUser();

        Long totalSent = messageRepository.countSentMessages(currentUser.getId());
        Long totalReceived = messageRepository.countReceivedMessages(currentUser.getId());
        Long unreadCount = messageRepository.countUnreadMessages(currentUser.getId());
        Long totalConversations = messageRepository.countConversations(currentUser.getId());

        return MessageStatisticsDto.builder()
                .totalSentMessages(totalSent)
                .totalReceivedMessages(totalReceived)
                .unreadMessagesCount(unreadCount)
                .totalConversations(totalConversations)
                .build();
    }

    @Transactional(readOnly = true)
    public Long getTotalUnreadCount(Long userId) {
        return messageRepository.countUnreadMessages(userId);
    }

    @Transactional(readOnly = true)
    public ConversationListDto getUserConversations(Long userId, int page, int size) {
        // Используем уже существующий метод getConversations()
        return getConversations(page, size);
    }

    // Приватные методы

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // ✅ ДОБАВИТЬ проверки
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    private void validateCanSendMessage(User sender, User recipient) {
        // Здесь добавить логику проверки:
        // - не заблокирован ли отправитель получателем
        // - не является ли профиль получателя приватным
        // - есть ли права на отправку сообщений

        // Пример базовой проверки
        if (sender.getId().equals(recipient.getId())) {
            throw new RuntimeException("Нельзя отправить сообщение самому себе");
        }
    }

    private void updateDeliveryStatus(Long messageId, DeliveryStatus status) {
        // Асинхронно обновляем статус доставки
        messageRepository.findById(messageId).ifPresent(message -> {
            message.setDeliveryStatus(status);
            messageRepository.save(message);
        });
    }


    /**
     * Уведомление об обновлении счетчика непрочитанных через WebSocket
     */
    public void notifyUnreadCountUpdate(Long userId) {


        // ✅ ПРАВИЛЬНО: получаем unreadCount для конкретного пользователя
        Long unreadCount = messageRepository.countUnreadMessages(userId);

        // Используем mapper для создания уведомления
        ChatMessage unreadNotification = messageMapper.createUnreadCountNotification(userId, unreadCount.intValue());
        webSocketHandler.sendToUser(userId, unreadNotification);
    }

    /**
     * Вызывать после отправки сообщения
     */
    public void afterMessageSent(Long recipientId) {
        notifyUnreadCountUpdate(recipientId);
    }

    /**
     * Вызывать после прочтения сообщений
     */
    public void afterMessagesRead(Long userId) {
        notifyUnreadCountUpdate(userId);
    }
}