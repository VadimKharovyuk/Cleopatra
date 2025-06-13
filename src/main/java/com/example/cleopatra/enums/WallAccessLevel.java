package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum WallAccessLevel {
    PUBLIC("Все могут писать на стене"),          // Все могут писать на стене
    FRIENDS("Только подписчики могут писать"),    // Только подписчики могут писать
    PRIVATE("Только владелец может писать"),      // Никто не может писать (только владелец)
    DISABLED("Стена отключена");                  // Стена отключена

    private final String displayName;

    WallAccessLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
