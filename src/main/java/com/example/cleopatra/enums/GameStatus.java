package com.example.cleopatra.enums;

import lombok.Getter;
@Getter
public enum GameStatus {
    IN_PROGRESS("В процессе"),
    COMPLETED("Завершена успешно"),
    FAILED("Провалена"),
    ABANDONED("Покинута");

    private final String description;

    GameStatus(String description) {
        this.description = description;
    }

}
