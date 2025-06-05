package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum Status {
    OPEN("Открыто"),
    IN_PROGRESS("В работе"),
    RESOLVED("Решено"),
    CLOSED("Закрыто");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
