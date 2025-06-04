package com.example.cleopatra.dto.ChatMessage;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationListDto {

    private List<ConversationDto> conversations;

    // Пагинация
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
    private Integer size;
    private Boolean hasNext;
    private Boolean hasPrevious;

    // Общая статистика
    private Integer totalUnreadMessages;
    private Integer totalConversations;
}
