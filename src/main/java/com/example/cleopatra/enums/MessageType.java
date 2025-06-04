package com.example.cleopatra.enums;
import lombok.Getter;

@Getter
public enum MessageType {
    TEXT("Текст"),
    IMAGE("Изображение"),
    FILE("Файл"),
    AUDIO("Аудио"),
    VIDEO("Видео"),
    SYSTEM("Системное"); // Для служебных сообщений

    private final String displayName;

    MessageType(String displayName) {
        this.displayName = displayName;
    }
}
