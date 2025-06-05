//package com.example.cleopatra.controller;
//
//import com.example.cleopatra.dto.ChatMessage.*;
//import com.example.cleopatra.model.User;
//import com.example.cleopatra.service.MessageService;
//import com.example.cleopatra.service.UserService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//
//@Controller
//@RequestMapping("/messages")
//@RequiredArgsConstructor
//@Slf4j
//public class MessagesController {
//
//
//    private final MessageService messageService;
//    private final UserService userService;
//
//    /**
//     * Главная страница всех чатов пользователя
//     */
//    @GetMapping
//    public String allChats(Model model, Authentication authentication,
//                           @RequestParam(defaultValue = "0") int page,
//                           @RequestParam(defaultValue = "20") int size) {
//
//        log.info("Открытие страницы всех чатов");
//
//        try {
//            User currentUser = getCurrentUser(authentication);
//            model.addAttribute("currentUser", currentUser);
//            model.addAttribute("currentUserId", currentUser.getId());
//            model.addAttribute("activeSection", "messages"); // Для sidebar
//
//            // Получаем список всех конверсаций используя правильный метод
//            ConversationListDto conversations = messageService.getConversations(page, size);
//            model.addAttribute("conversations", conversations);
//
//            // Общее количество непрочитанных сообщений
//            Long totalUnread = messageService.getUnreadMessagesCount();
//            model.addAttribute("totalUnread", totalUnread.intValue());
//
//            // Статистика для интерфейса
//            model.addAttribute("totalConversations", conversations.getTotalConversations());
//            model.addAttribute("hasConversations", conversations.getTotalConversations() > 0);
//
//            log.info("Загружено {} конверсаций для пользователя {}",
//                    conversations.getConversations().size(), currentUser.getId());
//
//        } catch (Exception e) {
//            log.error("Ошибка при загрузке чатов: {}", e.getMessage(), e);
//            model.addAttribute("error", "Не удалось загрузить чаты. Попробуйте позже.");
//            model.addAttribute("conversations", ConversationListDto.builder()
//                    .conversations(java.util.Collections.emptyList())
//                    .totalConversations(0)
//                    .totalUnreadMessages(0)
//                    .build());
//            model.addAttribute("totalUnread", 0);
//        }
//
//        return "messages/all-chats";
//    }
//
//    /**
//     * Страница конкретного чата с пользователем
//     */
//    @GetMapping("/chat/{userId}")
//    public String chatWithUser(@PathVariable Long userId,
//                               Model model,
//                               Authentication authentication,
//                               @RequestParam(defaultValue = "0") int page,
//                               @RequestParam(defaultValue = "50") int size) {
//
//        log.info("Открытие чата с пользователем {}", userId);
//
//        try {
//            User currentUser = getCurrentUser(authentication);
//            model.addAttribute("currentUser", currentUser);
//            model.addAttribute("currentUserId", currentUser.getId());
//            model.addAttribute("activeSection", "messages");
//
//            // Получаем сообщения конверсации
//            MessageListDto conversation = messageService.getConversation(userId, page, size);
//            model.addAttribute("conversation", conversation);
//            model.addAttribute("otherUser", conversation.getOtherUser());
//            model.addAttribute("otherUserId", userId);
//
//            // Отмечаем сообщения как прочитанные
//            messageService.markMessagesAsRead(userId);
//
//            // Дополнительная информация для интерфейса
//            model.addAttribute("hasMessages", !conversation.getMessages().isEmpty());
//            model.addAttribute("canLoadMore", conversation.getHasNext());
//
//            log.info("Загружено {} сообщений в чате с пользователем {}",
//                    conversation.getMessages().size(), userId);
//
//        } catch (Exception e) {
//            log.error("Ошибка при загрузке чата с пользователем {}: {}", userId, e.getMessage(), e);
//            model.addAttribute("error", "Не удалось загрузить чат. Попробуйте позже.");
//            return "messages/error";
//        }
//
//
//        return "messages/chat";
//    }
//
//    /**
//     * AJAX загрузка дополнительных чатов (для пагинации)
//     */
//    @GetMapping("/load-more")
//    @ResponseBody
//    public ResponseEntity<ConversationListDto> loadMoreChats(Authentication authentication,
//                                                             @RequestParam int page,
//                                                             @RequestParam(defaultValue = "20") int size) {
//        try {
//            ConversationListDto conversations = messageService.getConversations(page, size);
//            return ResponseEntity.ok(conversations);
//        } catch (Exception e) {
//            log.error("Ошибка при загрузке дополнительных чатов: {}", e.getMessage(), e);
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    /**
//     * AJAX загрузка дополнительных сообщений в чате
//     */
//    @GetMapping("/chat/{userId}/load-more")
//    @ResponseBody
//    public ResponseEntity<MessageListDto> loadMoreMessages(@PathVariable Long userId,
//                                                           @RequestParam int page,
//                                                           @RequestParam(defaultValue = "50") int size) {
//        try {
//            MessageListDto messages = messageService.getConversation(userId, page, size);
//            return ResponseEntity.ok(messages);
//        } catch (Exception e) {
//            log.error("Ошибка при загрузке дополнительных сообщений: {}", e.getMessage(), e);
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    /**
//     * AJAX отправка сообщения
//     */
//    @PostMapping("/send")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody MessageCreateDto createDto,
//                                                           Authentication authentication) {
//        try {
//            MessageResponseDto sentMessage = messageService.sendMessage(createDto);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", sentMessage);
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", false);
//            response.put("error", e.getMessage());
//
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * AJAX редактирование сообщения
//     */
//    @PutMapping("/edit/{messageId}")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> editMessage(@PathVariable Long messageId,
//                                                           @RequestBody MessageEditDto editDto) {
//        try {
//            MessageResponseDto editedMessage = messageService.editMessage(messageId, editDto);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", editedMessage);
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Ошибка при редактировании сообщения: {}", e.getMessage(), e);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", false);
//            response.put("error", e.getMessage());
//
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * AJAX удаление сообщения
//     */
//    @DeleteMapping("/delete/{messageId}")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long messageId,
//                                                             @RequestParam(defaultValue = "false") boolean deleteForEveryone) {
//        try {
//            messageService.deleteMessage(messageId, deleteForEveryone);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", "Сообщение удалено");
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Ошибка при удалении сообщения: {}", e.getMessage(), e);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", false);
//            response.put("error", e.getMessage());
//
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * AJAX отметка сообщений как прочитанных
//     */
//    @PostMapping("/mark-read/{senderId}")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long senderId) {
//        try {
//            messageService.markMessagesAsRead(senderId);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", "Сообщения отмечены как прочитанные");
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Ошибка при отметке сообщений как прочитанных: {}", e.getMessage(), e);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", false);
//            response.put("error", e.getMessage());
//
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * AJAX уведомление о печати
//     */
//    @PostMapping("/typing/{recipientId}")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> sendTypingNotification(@PathVariable Long recipientId,
//                                                                      @RequestParam(defaultValue = "true") boolean isTyping) {
//        try {
//            messageService.sendTypingNotification(recipientId, isTyping);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Ошибка при отправке уведомления о печати: {}", e.getMessage(), e);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", false);
//            response.put("error", e.getMessage());
//
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * AJAX поиск сообщений
//     */
//    @PostMapping("/search")
//    @ResponseBody
//    public ResponseEntity<MessageListDto> searchMessages(@RequestBody MessageSearchDto searchDto) {
//        try {
//            MessageListDto results = messageService.searchMessages(searchDto);
//            return ResponseEntity.ok(results);
//        } catch (Exception e) {
//            log.error("Ошибка при поиске сообщений: {}", e.getMessage(), e);
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    /**
//     * AJAX получение статистики сообщений
//     */
//    @GetMapping("/statistics")
//    @ResponseBody
//    public ResponseEntity<MessageStatisticsDto> getStatistics() {
//        try {
//            MessageStatisticsDto statistics = messageService.getMessageStatistics();
//            return ResponseEntity.ok(statistics);
//        } catch (Exception e) {
//            log.error("Ошибка при получении статистики: {}", e.getMessage(), e);
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    /**
//     * AJAX получение количества непрочитанных сообщений
//     */
//    @GetMapping("/unread-count")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> getUnreadCount() {
//        try {
//            Long unreadCount = messageService.getUnreadMessagesCount();
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("unreadCount", unreadCount);
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Ошибка при получении количества непрочитанных: {}", e.getMessage(), e);
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    /**
//     * Страница создания нового чата
//     */
//    @GetMapping("/new")
//    public String newChat(Model model, Authentication authentication) {
//        try {
//            User currentUser = getCurrentUser(authentication);
//            model.addAttribute("currentUser", currentUser);
//            model.addAttribute("activeSection", "messages");
//
////             Здесь можно добавить список контактов для выбора получателя
////             List<UserBriefDto> contacts = userService.getUserContacts(currentUser.getId());
////             model.addAttribute("contacts", contacts);
//
//            return "messages/new-chat";
//        } catch (Exception e) {
//            log.error("Ошибка при открытии страницы нового чата: {}", e.getMessage(), e);
//            model.addAttribute("error", "Не удалось открыть страницу создания чата");
//            return "messages/error";
//        }
//    }
//
//    /**
//     * Вспомогательный метод для получения текущего пользователя
//     */
//    private User getCurrentUser(Authentication authentication) {
//        return userService.getCurrentUserEntity(authentication);
//    }
//}

package com.example.cleopatra.controller;

import com.example.cleopatra.dto.ChatMessage.*;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.MessageService;
import com.example.cleopatra.service.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MessagesController {

    private final MessageService messageService;
    private final UserService userService;

    // Константы для пагинации
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_CHAT_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Главная страница всех чатов пользователя
     */
    @GetMapping
    public String allChats(Model model,
                           Authentication authentication,
                           @RequestParam(defaultValue = "0") @Min(0) int page,
                           @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {

        log.info("Открытие страницы всех чатов для пользователя: {}", authentication.getName());

        try {
            User currentUser = getCurrentUser(authentication);
            addCommonAttributes(model, currentUser);

            // Получаем список всех конверсаций
            ConversationListDto conversations = messageService.getConversations(page, size);
            model.addAttribute("conversations", conversations);


            // Общее количество непрочитанных сообщений
            Long totalUnread = messageService.getUnreadMessagesCount();
            model.addAttribute("totalUnread", totalUnread.intValue());

            // Статистика для интерфейса
            addConversationAttributes(model, conversations);

            log.info("Загружено {} конверсаций для пользователя {}",
                    conversations.getConversations().size(), currentUser.getId());

        } catch (Exception e) {
            log.error("Ошибка при загрузке чатов: {}", e.getMessage(), e);
            handleError(model, "Не удалось загрузить чаты. Попробуйте позже.");
        }

        return "messages/all-chats";
    }

    /**
     * Страница конкретного чата с пользователем
     */
    @GetMapping("/chat/{userId}")
    public String chatWithUser(@PathVariable Long userId,
                               Model model,
                               Authentication authentication) {

        log.info("Открытие чата с пользователем {}", userId);

        try {
            User currentUser = getCurrentUser(authentication);
            addCommonAttributes(model, currentUser);

            // Всегда загружаем последние сообщения (page = 0)
            MessageListDto conversation = messageService.getConversation(userId, 0, 50);

            model.addAttribute("conversation", conversation);
            model.addAttribute("otherUser", conversation.getOtherUser());
            model.addAttribute("otherUserId", userId);

            messageService.markMessagesAsRead(userId);

            model.addAttribute("hasMessages", !conversation.getMessages().isEmpty());
            model.addAttribute("canLoadMore", conversation.getHasNext());

            log.info("Загружено {} сообщений в чате с пользователем {}",
                    conversation.getMessages().size(), userId);

        } catch (Exception e) {
            log.error("Ошибка при загрузке чата с пользователем {}: {}", userId, e.getMessage(), e);
            model.addAttribute("error", "Не удалось загрузить чат. Попробуйте позже.");
            return "messages/error";
        }

        return "messages/chat";
    }

    /**
     * AJAX загрузка дополнительных чатов (для пагинации)
     */
    @GetMapping("/load-more")
    @ResponseBody
    public ResponseEntity<ConversationListDto> loadMoreChats(
            Authentication authentication,
            @RequestParam @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        try {
            log.debug("Загрузка дополнительных чатов: page={}, size={}", page, size);
            ConversationListDto conversations = messageService.getConversations(page, size);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("Ошибка при загрузке дополнительных чатов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * AJAX загрузка дополнительных сообщений в чате
     */
    @GetMapping("/chat/{userId}/load-more")
    @ResponseBody
    public ResponseEntity<MessageListDto> loadMoreMessages(
            @PathVariable Long userId,
            @RequestParam String beforeTimestamp,
            @RequestParam(defaultValue = "50") int size) {

        try {
            log.debug("Загрузка старых сообщений для пользователя {} до {}", userId, beforeTimestamp);

            // ✅ ИСПОЛЬЗУЕМ новый метод из сервиса
            MessageListDto result = messageService.loadMoreMessagesByTimestamp(userId, beforeTimestamp, size);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Ошибка загрузки старых сообщений для пользователя {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * AJAX отправка сообщения
     */
    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<ApiResponse<MessageResponseDto>> sendMessage(
            @Valid @RequestBody MessageCreateDto createDto,
            Authentication authentication) {

        try {
            log.debug("Отправка сообщения от {} к {}", authentication.getName(), createDto.getRecipientId());
            MessageResponseDto sentMessage = messageService.sendMessage(createDto);

            return ResponseEntity.ok(ApiResponse.success(sentMessage, "Сообщение отправлено"));
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * AJAX редактирование сообщения
     */
    @PutMapping("/edit/{messageId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<MessageResponseDto>> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody MessageEditDto editDto) {

        try {
            log.debug("Редактирование сообщения {}", messageId);
            MessageResponseDto editedMessage = messageService.editMessage(messageId, editDto);

            return ResponseEntity.ok(ApiResponse.success(editedMessage, "Сообщение отредактировано"));
        } catch (Exception e) {
            log.error("Ошибка при редактировании сообщения {}: {}", messageId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * AJAX удаление сообщения
     */
    @DeleteMapping("/delete/{messageId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam(defaultValue = "false") boolean deleteForEveryone) {

        try {
            log.debug("Удаление сообщения {}, deleteForEveryone={}", messageId, deleteForEveryone);
            messageService.deleteMessage(messageId, deleteForEveryone);

            String message = deleteForEveryone ? "Сообщение удалено у всех" : "Сообщение удалено";
            return ResponseEntity.ok(ApiResponse.success(null, message));
        } catch (Exception e) {
            log.error("Ошибка при удалении сообщения {}: {}", messageId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * AJAX отметка сообщений как прочитанных
     */
    @PostMapping("/mark-read/{senderId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long senderId) {
        try {
            log.debug("Отметка сообщений как прочитанных от пользователя {}", senderId);
            messageService.markMessagesAsRead(senderId);

            return ResponseEntity.ok(ApiResponse.success(null, "Сообщения отмечены как прочитанные"));
        } catch (Exception e) {
            log.error("Ошибка при отметке сообщений как прочитанных от {}: {}", senderId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * AJAX уведомление о печати
     */
    @PostMapping("/typing/{recipientId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> sendTypingNotification(
            @PathVariable Long recipientId,
            @RequestParam(defaultValue = "true") boolean isTyping) {

        try {
            messageService.sendTypingNotification(recipientId, isTyping);
            return ResponseEntity.ok(ApiResponse.success(null, "Уведомление отправлено"));
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления о печати: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * AJAX поиск сообщений
     */
    @PostMapping("/search")
    @ResponseBody
    public ResponseEntity<MessageListDto> searchMessages(@Valid @RequestBody MessageSearchDto searchDto) {
        try {
            log.debug("Поиск сообщений: {}", searchDto.getSearchText());
            MessageListDto results = messageService.searchMessages(searchDto);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Ошибка при поиске сообщений: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * AJAX получение статистики сообщений
     */
    @GetMapping("/statistics")
    @ResponseBody
    public ResponseEntity<MessageStatisticsDto> getStatistics() {
        try {
            MessageStatisticsDto statistics = messageService.getMessageStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCountApi() {
        try {
            Long unreadCount = messageService.getUnreadMessagesCount();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", unreadCount.intValue()
            ));
        } catch (Exception e) {
            log.error("Ошибка при получении счетчика непрочитанных: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "count", 0
            ));
        }
    }

    /**
     * Страница создания нового чата
     */
    @GetMapping("/new")
    public String newChat(Model model, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            addCommonAttributes(model, currentUser);

            // TODO: Добавить список контактов для выбора получателя
            // List<UserBriefDto> contacts = userService.getUserContacts(currentUser.getId());
            // model.addAttribute("contacts", contacts);

            return "messages/new-chat";
        } catch (Exception e) {
            log.error("Ошибка при открытии страницы нового чата: {}", e.getMessage(), e);
            model.addAttribute("error", "Не удалось открыть страницу создания чата");
            return "messages/error";
        }
    }

    // Вспомогательные методы

    /**
     * Получение текущего пользователя
     */
    private User getCurrentUser(Authentication authentication) {
        return userService.getCurrentUserEntity(authentication);
    }

    /**
     * Добавление общих атрибутов в модель
     */
    private void addCommonAttributes(Model model, User currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("activeSection", "messages");
    }

    /**
     * Добавление атрибутов конверсации в модель
     */
    private void addConversationAttributes(Model model, ConversationListDto conversations) {
        model.addAttribute("totalConversations", conversations.getTotalConversations());
        model.addAttribute("hasConversations", conversations.getTotalConversations() > 0);
    }

    /**
     * Обработка ошибок для страниц
     */
    private void handleError(Model model, String errorMessage) {
        model.addAttribute("error", errorMessage);
        model.addAttribute("conversations", ConversationListDto.builder()
                .conversations(Collections.emptyList())
                .totalConversations(0)
                .totalUnreadMessages(0)
                .build());
        model.addAttribute("totalUnread", 0);
    }

    // Внутренние классы для ответов API

    /**
     * Универсальный класс ответа API
     */
    @Getter
    public static class ApiResponse<T> {
        // Getters
        private boolean success;
        private String message;
        private T data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

    }

    /**
     * Класс ответа для количества непрочитанных сообщений
     */
    @Getter
    public static class UnreadCountResponse {
        private Long unreadCount;

        public UnreadCountResponse(Long unreadCount) {
            this.unreadCount = unreadCount;
        }

    }
}