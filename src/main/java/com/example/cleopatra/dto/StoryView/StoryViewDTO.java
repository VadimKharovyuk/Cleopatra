package com.example.cleopatra.dto.StoryView;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryViewDTO {

    private Long id;

    private Long storyId;

    // Информация о просмотревшем пользователе
    private Long viewerId;
    private String viewerFirstName;
    private String viewerLastName;
    private String viewerImageUrl;

    // Время просмотра
    private LocalDateTime viewedAt;

    // Вспомогательные поля для UI
    private String timeAgo; // "2 часа назад"

    // Полное имя для удобства
    public String getViewerFullName() {
        if (viewerFirstName == null && viewerLastName == null) {
            return "Пользователь"; // fallback
        }

        StringBuilder fullName = new StringBuilder();
        if (viewerFirstName != null) {
            fullName.append(viewerFirstName);
        }
        if (viewerLastName != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(viewerLastName);
        }

        return fullName.toString();
    }
}