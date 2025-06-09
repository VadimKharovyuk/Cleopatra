package com.example.cleopatra.dto.StoryDTO;

import com.example.cleopatra.dto.StoryView.StoryViewDTO;
import com.example.cleopatra.enums.StoryEmoji;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryDTO {

    private Long id;

    // Информация о владельце истории
    private Long userId;
    private String userFirstName;
    private String userLastName;
    private String userImageUrl;

    // Данные изображения (без самого byte[])
    private String imageId;
    private String imageUrl; // URL для получения изображения (/api/stories/image/{imageId})
    private String contentType;

    // Контент истории
    private StoryEmoji emoji;
    private String description;

    // Время
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String timeAgo; // "2 часа назад"
    private String expiresIn; // "истекает через 5 часов"

    // Статистика просмотров
    private Long viewsCount;
    private List<StoryViewDTO> recentViews; // Последние 3-5 просмотров для превью

    // Статус для текущего пользователя
    private Boolean isViewedByCurrentUser; // Просматривал ли текущий пользователь
    private Boolean isOwner; // Является ли текущий пользователь владельцем
    private Boolean isExpired; // Истекла ли история

    // Полное имя владельца для удобства
    public String getUserFullName() {
        if (userFirstName == null && userLastName == null) {
            return "Пользователь"; // fallback
        }

        StringBuilder fullName = new StringBuilder();
        if (userFirstName != null) {
            fullName.append(userFirstName);
        }
        if (userLastName != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(userLastName);
        }

        return fullName.toString();
    }

    // Проверка, скоро ли истечет (меньше 2 часов)
    public Boolean isExpiringSoon() {
        if (expiresAt == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursFromNow = now.plusHours(2);
        return expiresAt.isBefore(twoHoursFromNow);
    }
}
