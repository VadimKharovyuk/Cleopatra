package com.example.cleopatra.dto.Forum;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ForumPageResponseDTO {
    private List<ForumPageCardDTO> content;


    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    // Дополнительные метаданные
    private String sortBy;
    private String sortDirection;
}
