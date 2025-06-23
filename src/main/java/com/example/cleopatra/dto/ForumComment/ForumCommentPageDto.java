package com.example.cleopatra.dto.ForumComment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumCommentPageDto {

    private List<ForumCommentDto> comments;
    private boolean hasNext;
    private boolean hasPrevious;
    private int numberOfElements;
    private int size;

    // Для cursor-based пагинации (опционально)
    private String nextCursor;
    private String previousCursor;
}
