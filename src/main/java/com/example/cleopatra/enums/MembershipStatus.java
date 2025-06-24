package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum MembershipStatus {
    PENDING("⏳ Ожидает", "Заявка на вступление ожидает рассмотрения"),
    APPROVED("✅ Одобрено", "Заявка одобрена, пользователь является активным участником"),
    REJECTED("❌ Отклонено", "Заявка на вступление была отклонена"),
    BANNED("🚫 Заблокирован", "Пользователь заблокирован в данной группе"),
    LEFT("👋 Покинул", "Пользователь покинул группу (для истории)");

    private final String displayName;
    private final String description;

    MembershipStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}