package com.example.cleopatra.dto.WallPost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WallPostCardResponse {
    private Long id;
    private String text;
    private String picUrl;
    private UserBriefResponse author;
    private UserBriefResponse wallOwner;
    private Long likesCount;
    private Long commentsCount;
    private LocalDateTime createdAt;

    // Права доступа для текущего пользователя
    private Boolean canEdit;    // может ли редактировать (только автор)
    private Boolean canDelete;  // может ли удалить (автор или владелец стены)
}
