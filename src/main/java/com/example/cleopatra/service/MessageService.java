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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        User recipient = userRepository.findByIdWithOnlineStatus(createDto.getRecipientId())
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
    /**
     * Получить список сообщений в конверсации с правильной пагинацией
     */
    @Transactional(readOnly = true)
    public MessageListDto getConversation(Long otherUserId, int page, int size) {
        User currentUser = getCurrentUser(); // уже оптимизирован выше

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

        // ✅ ЗАМЕНИТЬ НА ОПТИМИЗИРОВАННЫЙ МЕТОД
        User otherUser = userRepository.findByIdWithOnlineStatus(otherUserId)
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

    // В MessageService замените этот метод:
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        String username = authentication.getName();

        // ✅ ЗАМЕНИТЬ НА ОПТИМИЗИРОВАННЫЙ МЕТОД
        return userRepository.findByEmailWithOnlineStatus(username)
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




    public long getTotalMessagesCount() {
        return messageRepository.count();
    }


    public long getMessagesCountFromDate(LocalDate fromDate) {
        LocalDateTime startDate = fromDate.atStartOfDay();
        return messageRepository.countByCreatedAtGreaterThanEqual(startDate);
    }


    public long getMessagesCountByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return messageRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }


    public long getMessagesCountBetweenDates(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return messageRepository.countByCreatedAtBetween(start, end);
    }

    // Дополнительный метод для подсчета сообщений за сегодня
    public long getTodayMessagesCount() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return messageRepository.countTodayMessages(startOfDay, endOfDay);
    }
}