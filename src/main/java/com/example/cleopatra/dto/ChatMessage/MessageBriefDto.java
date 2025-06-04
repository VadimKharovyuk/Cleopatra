package com.example.cleopatra.dto.ChatMessage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageBriefDto {

    private Long id;
    private UserBriefDto sender;
    private String content;
    private String shortContent; // Первые 50 символов
}
