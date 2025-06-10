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

    // Информация об истории
    private Long storyId;

    // Информация о просматривающем пользователе
    private Long viewerId;
    private String viewerFirstName;
    private String viewerLastName;
    private String viewerImageUrl;
    private String viewerImgId;

    // Время просмотра
    private LocalDateTime viewedAt;
    private LocalDateTime createdAt;

    // Вспомогательные методы
    public String getViewerFullName() {
        if (viewerFirstName == null && viewerLastName == null) {
            return "Неизвестный пользователь";
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

    public String getViewerDisplayName() {
        String fullName = getViewerFullName();
        return fullName.isEmpty() ? "Пользователь #" + viewerId : fullName;
    }
}