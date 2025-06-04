package com.example.cleopatra.dto.ChatMessage;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusMessage {
    private Long userId;
    private String status; // ONLINE, OFFLINE, TYPING
    private LocalDateTime timestamp;
}