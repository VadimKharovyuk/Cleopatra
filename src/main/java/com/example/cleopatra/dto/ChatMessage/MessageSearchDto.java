package com.example.cleopatra.dto.ChatMessage;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageSearchDto {

    private String searchText;
    private Long otherUserId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Boolean unreadOnly;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}
