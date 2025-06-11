package com.example.cleopatra.enums;

import lombok.Getter;
@Getter
public enum AdStatus {
    PENDING("На модерации"),
    APPROVED("Одобрена"),
    ACTIVE("Активна"),
    PAUSED("Приостановлена"),
    REJECTED("Отклонена"),
    BLOCKED("Заблокирована"),
    FINISHED("Завершена");

    private final String displayName;

    AdStatus(String displayName) {
        this.displayName = displayName;
    }

}
