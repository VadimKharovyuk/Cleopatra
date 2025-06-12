package com.example.cleopatra.EVENT;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnsubscribeEvent {
    private final Long subscriberId;     // кто отписался
    private final Long subscribedToId;   // от кого отписались
    private final String subscriberName; // имя того, кто отписался
}
