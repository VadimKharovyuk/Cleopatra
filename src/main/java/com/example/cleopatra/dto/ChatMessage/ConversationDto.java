package com.example.cleopatra.dto.ChatMessage;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationDto {

    // Собеседник
    private UserBriefDto otherUser;

    // Последнее сообщение
    private MessageResponseDto lastMessage;

    // Количество непрочитанных сообщений
    private Integer unreadCount;

    // Время последней активности
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivity;

    // Заблокирована ли конверсация
    private Boolean isBlocked;

    // Статус собеседника
    private Boolean isOtherUserOnline;
    private String otherUserStatus;
}
