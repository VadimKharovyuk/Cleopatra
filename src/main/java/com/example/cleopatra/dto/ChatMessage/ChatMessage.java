package com.example.cleopatra.dto.ChatMessage;

import com.example.cleopatra.enums.DeliveryStatus;
import com.example.cleopatra.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class ChatMessage {
    private Long id;
    private Long senderId;
    private Long recipientId;
    private String content;
    private MessageType type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private Boolean isRead;
    private String senderName;
    private String recipientName;


    private MessageBriefDto replyToMessage;

    // ДОБАВИТЬ ЭТО ПОЛЕ для дополнительной информации:
    private DeliveryStatus deliveryStatus;
    private Boolean isEdited;

    // Дополнительные поля для WebSocket
    private String action; // SEND, EDIT, DELETE, TYPING_START, TYPING_STOP
}
