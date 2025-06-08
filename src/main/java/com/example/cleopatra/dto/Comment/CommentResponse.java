package com.example.cleopatra.dto.Comment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// === DTO для ответа с информацией о комментарии ===
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private String content;

    // Информация об авторе
    private CommentAuthorDto author;

    // Информация о посте (минимальная)
    private Long postId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Вложенный класс для информации об авторе
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentAuthorDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String imageUrl;

        // Полное имя для удобства
        public String getFullName() {
            if (firstName == null && lastName == null) {
                return "Анонимный пользователь";
            }
            return String.format("%s %s",
                    firstName != null ? firstName : "",
                    lastName != null ? lastName : "").trim();
        }
    }
}
