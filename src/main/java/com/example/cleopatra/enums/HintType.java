package com.example.cleopatra.enums;


import lombok.Getter;
@Getter
public enum HintType {
    FIFTY_FIFTY("50/50", "Убрать 2 неверных ответа"),
    SKIP_QUESTION("Skip", "Пропустить вопрос");

    private final String code;
    private final String description;

    HintType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
