package com.example.cleopatra.dto.ProjectNews;

import com.example.cleopatra.enums.NewsType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectNewsResponse {

    private Long id;
    private String title;
    private String description; // Полное описание
    private String shortDescription;
    private String photoUrl;
    private String photoId;
    private NewsType newsType;
    private String newsTypeDisplayName;
    private Long viewsCount;
    private Boolean isPublished;

    // Информация об авторе
    private AuthorInfo author;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String imageUrl;
    }
}