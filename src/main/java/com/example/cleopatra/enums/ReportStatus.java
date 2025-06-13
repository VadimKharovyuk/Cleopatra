package com.example.cleopatra.enums;


import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("Ожидает рассмотрения"),
    UNDER_REVIEW("На рассмотрении"),
    RESOLVED("Решена"),
    REJECTED("Отклонена"),
    ESCALATED("Передана выше");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}