package com.example.cleopatra.enums;

import lombok.Getter;
@Getter
public enum GroupStatus {
    ACTIVE("🟢 Активная", "Группа функционирует в обычном режиме"),
    SUSPENDED("⚠️ Приостановлена", "Группа временно приостановлена администратором системы"),
    DELETED("🗑️ Удалена", "Группа удалена (мягкое удаление)"),
    ARCHIVED("📦 Архивирована", "Группа архивирована владельцем");

    private final String displayName;
    private final String description;

    GroupStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
