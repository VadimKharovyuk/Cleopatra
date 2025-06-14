package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum NewsType {
    UPDATE("Обновление"),
    SECURITY("Безопасность"),
    BUG_FIX("Исправление багов"),
    NEW_FEATURE("Новые функции"),
    ANNOUNCEMENT("Объявление"),
    MAINTENANCE("Техническое обслуживание"),
    POLICY_CHANGE("Изменение политики"),
    EVENT("События"),
    PROMOTION("Акции");

    private final String displayName;

    NewsType(String displayName) {
        this.displayName = displayName;
    }

}

