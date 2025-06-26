package com.example.cleopatra.dto.GroupDto;


import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupPostResponse {

    private Long id;
    private String text;
    private String imageUrl;
    private String imgId;
    private Long groupId;
    private Long authorId;
    private String authorName;
    private String authorImageUrl;
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasImage;
}
