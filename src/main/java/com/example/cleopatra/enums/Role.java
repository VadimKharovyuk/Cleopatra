package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum Role {

    ADMIN("Администратор", "Руководство группы"),

    PERFORMER("Исполнитель", "Основной артист группы"),

    USER("Пользователь", "Новичок/Стажер");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
