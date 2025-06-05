package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum Category {
    BUG("Ошибка"),
    FEATURE("Запрос функции"),
    ACCOUNT("Проблемы с аккаунтом"),
    TECHNICAL("Технические проблемы"),
    OTHER("Другое");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
