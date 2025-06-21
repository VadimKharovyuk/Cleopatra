package com.example.cleopatra.dto.Forum;

import com.example.cleopatra.enums.ForumType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ForumPageCardDTO {
    private Long id;
    private String title;
    private ForumType forumType;
    private Integer viewCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private String authorName;
}
