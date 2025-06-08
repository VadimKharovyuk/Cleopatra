package com.example.cleopatra.dto.Comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentActionResponse {
    private boolean success;
    private String message;
    private CommentResponse comment; // Опционально, если нужно вернуть обновленный комментарий

    // Статические методы для удобного создания ответов
    public static CommentActionResponse success(String message) {
        return CommentActionResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static CommentActionResponse success(String message, CommentResponse comment) {
        return CommentActionResponse.builder()
                .success(true)
                .message(message)
                .comment(comment)
                .build();
    }

    public static CommentActionResponse failure(String message) {
        return CommentActionResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
