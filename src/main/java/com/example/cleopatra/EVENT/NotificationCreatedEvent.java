package com.example.cleopatra.EVENT;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationCreatedEvent {
    private final Long notificationId;
}
