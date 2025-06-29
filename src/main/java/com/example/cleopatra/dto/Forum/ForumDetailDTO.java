package com.example.cleopatra.dto.Forum;

import com.example.cleopatra.enums.ForumType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ForumDetailDTO {
    private Long id;
    private String title;
    private String description;
    private ForumType forumType;
    private Integer viewCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Информация об авторе
    private String authorName;
    private String authorEmail;
    private String authorImageUrl;
    private Long authorId;



}