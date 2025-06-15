package com.example.cleopatra.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
@AllArgsConstructor
@Getter
public enum ProfileAccessLevel {
    PUBLIC(0, "Публичный", "Профиль виден всем пользователям"),
    SUBSCRIBERS_ONLY(1, "Только подписчики", "Профиль виден только подписчикам"),
    MUTUAL_SUBSCRIPTIONS(2, "Взаимные подписки", "Профиль виден при взаимной подписке"),
    PRIVATE(3, "Приватный", "Профиль полностью закрыт");

    private final int level;
    private final String displayName;
    private final String description;



    /**
     * Проверяет, является ли текущий уровень более строгим чем переданный
     */
    public boolean isStricterThan(ProfileAccessLevel other) {
        return this.level > other.level;
    }

    /**
     * Проверяет, является ли текущий уровень менее строгим чем переданный
     */
    public boolean isLessStrictThan(ProfileAccessLevel other) {
        return this.level < other.level;
    }
}