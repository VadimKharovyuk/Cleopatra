package com.example.cleopatra.enums;


import lombok.Getter;

@Getter
public enum GroupType {
    OPEN("🌍 Открытая", "Свободное вступление для всех пользователей"),
    CLOSED("🔒 Закрытая", "Вступление только после одобрения администратора"),
    PRIVATE("👁️ Приватная", "Вступление только по персональным приглашениям");

    private final String displayName;
    private final String description;

    GroupType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
