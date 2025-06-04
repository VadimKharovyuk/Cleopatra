package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum DeliveryStatus {
    SENT("Отправлено"),
    DELIVERED("Доставлено"),
    READ("Прочитано"),
    FAILED("Ошибка доставки");

    private final String displayName;

    DeliveryStatus(String displayName) {
        this.displayName = displayName;
    }

}
