package com.example.cleopatra.dto.Post;

import com.example.cleopatra.dto.Location.LocationDto;
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
public class PostCardDto {


    private Long id;
    private String content;
    private String imageUrl;
    private AuthorDto author;
    private LocalDateTime createdAt;
    private Long likesCount;
    private Long commentsCount;
    private Long viewsCount;



    // Дополнительные поля для UI
    private Boolean hasImage;
    private Boolean isLongContent; // true если контент обрезан

    // ✅ ДОБАВИТЬ поля для лайков
    private Boolean isLikedByCurrentUser; // Лайкнул ли текущий пользователь
    private List<LikeUserDto> recentLikes; // Последние 2-3 пользователя для карточки



    private LocationDto location;
    private Boolean hasLocation;

    public Boolean hasLocation() {
        return location != null && location.hasValidLocation();
    }


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