package com.example.cleopatra.dto.GroupDto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupPostDetails {

    private Long id;
    private String text;
    private String imageUrl;
    private LocalDateTime createdAt;

    // Данные автора
    private Long authorId;
    private String authorName;
    private String authorImageUrl;

    // Счетчики
    private Long likeCount;
    private Long commentCount;

    // Дополнительная информация для UI
    private boolean hasImage;
    private boolean isLikedByCurrentUser;



}