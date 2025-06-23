package com.example.cleopatra.dto.ForumComment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumCommentStatsDto {

    private Long forumId;
    private Integer totalComments;
    private Integer topLevelComments;
    private Integer repliesCount;
    private LocalDateTime lastCommentAt;
    private String lastCommentAuthor;
}