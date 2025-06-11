package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum ReportReason {
    SPAM("Спам"),
    INAPPROPRIATE("Неподходящий контент"),
    VIOLENCE("Насилие"),
    ADULT_CONTENT("Контент 18+"),
    FRAUD("Мошенничество"),
    OTHER("Другое");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}