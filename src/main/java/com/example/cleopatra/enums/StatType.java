package com.example.cleopatra.enums;


import lombok.Getter;

@Getter
public enum StatType {
    VIEW("Просмотр"),
    CLICK("Клик"),
    IMPRESSION("Показ"), // когда реклама загрузилась, но не обязательно просмотрена
    CONVERSION("Конверсия"); // если отслеживаете действия после клика

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }
}
