package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum GroupRole {
    OWNER("👑 Владелец", "Создатель группы с полными правами управления"),
    ADMIN("⚡ Администратор", "Полные права модерации и управления участниками"),
    MODERATOR("🛡️ Модератор", "Ограниченные права модерации контента"),
    MEMBER("👤 Участник", "Обычный участник группы");

    private final String displayName;
    private final String description;

    GroupRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}