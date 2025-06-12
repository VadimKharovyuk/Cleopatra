package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    // Профиль
    PROFILE_VISIT("Посещение профиля", "👤"),
    FOLLOW("Новый подписчик", "👥"),
    UNFOLLOW("Отписка", "👥"),

    // Посты
    POST_LIKE("Лайк поста", "❤️"),
    POST_COMMENT("Комментарий к посту", "💬"),


    // Комментарии
    COMMENT_LIKE("Лайк комментария", "👍"),
    COMMENT_REPLY("Ответ на комментарий", "💬"),

    MENTION("Упоминание в посте", "@️⃣"),

    // Системные
    SYSTEM_ANNOUNCEMENT("Системное объявление", "📢"),
    SYSTEM_UPDATE("Обновление", "🔄"),

    // Другие
    FRIEND_REQUEST("Заявка в друзья", "🤝"),
    FRIEND_ACCEPT("Принятие заявки", "✅"),
    MESSAGE("Личное сообщение", "✉️");

    private final String description;
    private final String emoji;

    NotificationType(String description, String emoji) {
        this.description = description;
        this.emoji = emoji;
    }

}
