package com.example.cleopatra.dto.ForumComment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumCommentDto {

    private Long id;
    private String content;
    private Long forumId;
    private Long parentId;
    private Integer level;
    private Integer childrenCount;
    private Boolean deleted;


    // Данные автора
    private Long authorId;
    private String authorName;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Вспомогательные методы для фронта
    public boolean isReply() {
        return parentId != null;
    }

    public boolean hasChildren() {
        return childrenCount != null && childrenCount > 0;
    }

    public boolean isDeleted() {
        return deleted != null && deleted;
    }
}