package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.ChatMessage.*;
import com.example.cleopatra.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "API для работы с сообщениями")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "Отправить сообщение", description = "Отправить новое сообщение пользователю")
    public ResponseEntity<MessageResponseDto> sendMessage(
            @Valid @RequestBody MessageCreateDto createDto) {
        try {
            log.info("Sending message to user {}", createDto.getRecipientId());

            MessageResponseDto response = messageService.sendMessage(createDto);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending message: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/conversations/{userId}")
    @Operation(summary = "Получить сообщения конверсации",
            description = "Получить список сообщений в конверсации с пользователем")
    public ResponseEntity<MessageListDto> getConversation(
            @Parameter(description = "ID собеседника")
            @PathVariable Long userId,
            @Parameter(description = "Номер страницы (начиная с 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size) {
        try {
            MessageListDto response = messageService.getConversation(userId, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting conversation: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/conversations")
    @Operation(summary = "Получить список конверсаций",
            description = "Получить список всех конверсаций пользователя")
    public ResponseEntity<ConversationListDto> getConversations(
            @Parameter(description = "Номер страницы")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "10") int size) {
        try {
            ConversationListDto response = messageService.getConversations(page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting conversations: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{messageId}/read")
    @Operation(summary = "Отметить сообщения как прочитанные",
            description = "Отметить все сообщения от отправителя как прочитанные")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "ID отправителя")
            @PathVariable Long senderId) {
        try {
            messageService.markMessagesAsRead(senderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking messages as read: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{messageId}")
    @Operation(summary = "Редактировать сообщение",
            description = "Редактировать содержимое сообщения")
    public ResponseEntity<MessageResponseDto> editMessage(
            @Parameter(description = "ID сообщения")
            @PathVariable Long messageId,
            @Valid @RequestBody MessageEditDto editDto) {
        try {
            MessageResponseDto response = messageService.editMessage(messageId, editDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error editing message: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Удалить сообщение",
            description = "Удалить сообщение для себя или для всех")
    public ResponseEntity<Void> deleteMessage(
            @Parameter(description = "ID сообщения")
            @PathVariable Long messageId,
            @Parameter(description = "Удалить для всех")
            @RequestParam(defaultValue = "false") boolean deleteForEveryone) {
        try {
            messageService.deleteMessage(messageId, deleteForEveryone);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting message: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/search")
    @Operation(summary = "Поиск сообщений",
            description = "Найти сообщения по различным критериям")
    public ResponseEntity<MessageListDto> searchMessages(
            @Valid @RequestBody MessageSearchDto searchDto) {
        try {
            MessageListDto response = messageService.searchMessages(searchDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching messages: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/typing/{recipientId}")
    @Operation(summary = "Уведомление о наборе текста",
            description = "Отправить уведомление о том, что пользователь печатает")
    public ResponseEntity<Void> sendTypingNotification(
            @Parameter(description = "ID получателя")
            @PathVariable Long recipientId,
            @Parameter(description = "Печатает ли пользователь")
            @RequestParam(defaultValue = "true") boolean isTyping) {
        try {
            messageService.sendTypingNotification(recipientId, isTyping);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error sending typing notification: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Количество непрочитанных сообщений",
            description = "Получить общее количество непрочитанных сообщений")
    public ResponseEntity<Long> getUnreadCount() {
        try {
            Long count = messageService.getUnreadMessagesCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting unread count: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
