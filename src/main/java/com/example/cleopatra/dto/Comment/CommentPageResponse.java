package com.example.cleopatra.dto.Comment;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageResponse {

    private java.util.List<CommentResponse> comments;

    // Информация о пагинации
    private PageInfo pageInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int currentPage;
        private int pageSize;
        private boolean hasNext;
        private boolean hasPrevious;
        private long totalElements; // Общее количество комментариев к посту
        private boolean isFirst;
        private boolean isLast;
    }
}

