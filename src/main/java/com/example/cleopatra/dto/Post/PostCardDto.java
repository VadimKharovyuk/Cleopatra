package com.example.cleopatra.dto.Post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostCardDto {

    private Long id;
    private String content; // обрезанный контент для превью
    private String imageUrl;
    private AuthorDto author;
    private LocalDateTime createdAt;
    private Long likesCount;
    private Long commentsCount;
    private Long viewsCount;

    // Дополнительные поля для UI
    private Boolean hasImage;
    private Boolean isLongContent; // true если контент обрезан

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthorDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String imageUrl;

        // Полное имя для удобства
        public String getFullName() {
            return firstName + " " + lastName;
        }
    }
}