package com.example.cleopatra.enums;


import lombok.Getter;

@Getter
public enum AdCategory {
    BUSINESS("Бизнес"),
    ENTERTAINMENT("Развлечения"),
    EDUCATION("Образование"),
    TECHNOLOGY("Технологии"),
    FASHION("Мода"),
    FOOD("Еда"),
    TRAVEL("Путешествия"),
    HEALTH("Здоровье");

    private final String description;

    AdCategory(String description) {
        this.description = description;
    }
}
