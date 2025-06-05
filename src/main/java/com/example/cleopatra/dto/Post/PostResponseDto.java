package com.example.cleopatra.dto.Post;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponseDto {

    private Long id;
    private String content;
    private String imageUrl;
    private AuthorDto author;
    private LocalDateTime createdAt;
    private Long likesCount;
    private Long commentsCount;
    private Long viewsCount;

    // ✅ ДОБАВИТЬ поля для лайков
    private Boolean isLikedByCurrentUser; // Лайкнул ли текущий пользователь
    private List<LikeUserDto> recentLikes;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthorDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String imageUrl;
    }

    // ✅ ДОБАВИТЬ DTO для пользователей, которые лайкнули
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LikeUserDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String imageUrl;

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }
}
