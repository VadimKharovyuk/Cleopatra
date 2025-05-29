package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
    PENDING("Ожидает рассмотрения", "⏳", "#ffc107"),
    UNDER_REVIEW("На рассмотрении", "👀", "#17a2b8"),
    APPROVED("Одобрена", "✅", "#28a745"),
    REJECTED("Отклонена", "❌", "#dc3545"),
    CONTACTED("Связались", "📞", "#6f42c1"),
    HIRED("Принят на работу", "🎉", "#20c997");

    private final String displayName;
    private final String icon;
    private final String color;

    ApplicationStatus(String displayName, String icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }
}