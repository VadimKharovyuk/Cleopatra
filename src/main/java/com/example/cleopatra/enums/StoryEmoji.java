package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum StoryEmoji {
    HEART("❤️", "Сердце"),
    LIKE("👍", "Лайк"),
    LAUGH("😂", "Смех"),
    SURPRISE("😮", "Удивление"),
    LOVE("😍", "Влюбленность"),
    FIRE("🔥", "Огонь"),
    STAR("⭐", "Звезда"),
    CLAP("👏", "Аплодисменты"),
    PARTY("🎉", "Праздник"),
    COOL("😎", "Крутой");

    private final String emoji;
    private final String description;

    StoryEmoji(String emoji, String description) {
        this.emoji = emoji;
        this.description = description;
    }
}