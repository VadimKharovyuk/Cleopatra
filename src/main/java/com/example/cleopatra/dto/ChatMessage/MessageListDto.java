package com.example.cleopatra.dto.ChatMessage;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessageListDto {

    private List<MessageResponseDto> messages;

    // Информация о пагинации
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
    private Integer size;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Integer nextPage;
    private Integer previousPage;

    // Информация о конверсации
    private UserBriefDto otherUser;
    private Integer totalUnreadCount;
}
