package com.example.cleopatra.EVENT;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubscriptionCreatedEvent {
    private final Long subscriberId;     // кто подписался
    private final Long subscribedToId;   // на кого подписались
    private final String subscriberName; // имя подписчика (для уведомления)
}
