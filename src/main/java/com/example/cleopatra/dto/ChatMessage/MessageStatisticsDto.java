package com.example.cleopatra.dto.ChatMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatisticsDto {
    private Long totalSentMessages;
    private Long totalReceivedMessages;
    private Long unreadMessagesCount;
    private Long totalConversations;
    private Double averageMessagesPerDay;
    private Long messagesThisWeek;
    private Long messagesThisMonth;
}
