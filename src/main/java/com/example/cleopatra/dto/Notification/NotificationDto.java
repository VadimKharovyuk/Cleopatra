package com.example.cleopatra.dto.Notification;

import com.example.cleopatra.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private Long recipientId;
    private Long actorId;
    private String actorName;
    private String actorImageUrl;
    private NotificationType type;
    private String title;
    private String message;
    private String data;
    private Long relatedEntityId;
    private String relatedEntityType;
    private Boolean isRead;
    private Boolean isSent;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
