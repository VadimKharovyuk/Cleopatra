package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum PerformerProfile {
    DANCER("Танцор", "💃", "Профессиональный танцор различных стилей"),
    ACROBAT("Акробат", "🤸", "Акробатические элементы и трюки"),
    TRICKER("Трикер", "🤸‍♂️", "Трикинг и экстремальные трюки"),
    SINGER("Певец", "🎤", "Вокальное исполнение"),
    GYMNAST("Гимнаст", "🤸‍♀️", "Спортивная и художественная гимнастика"),
    JUGGLER("Жонглер", "🤹", "Жонглирование различными предметами"),
    MAGICIAN("Иллюзионист", "🎩", "Фокусы и иллюзии"),
    AERIAL_ARTIST("Воздушный гимнаст", "🎪", "Выступления на воздушных полотнах, кольце"),
    FIRE_PERFORMER("Огненное шоу", "🔥", "Выступления с огнем"),
    CONTORTIONIST("Контортионист", "🧘", "Гибкость и пластика тела"),
    CLOWN("Клоун", "🤡", "Комедийные и развлекательные номера"),
    MUSICIAN("Музыкант", "🎸", "Игра на музыкальных инструментах"),
    OTHER("Другое", "🎭", "Другие виды выступлений");

    private final String displayName;
    private final String icon;
    private final String description;

    PerformerProfile(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayNameWithIcon() {
        return icon + " " + displayName;
    }

    // Группировка по категориям для удобства
    public static PerformerProfile[] getDancers() {
        return new PerformerProfile[]{DANCER, ACROBAT, TRICKER, GYMNAST};
    }

    public static PerformerProfile[] getVocalists() {
        return new PerformerProfile[]{SINGER, MUSICIAN};
    }

    public static PerformerProfile[] getCircusArts() {
        return new PerformerProfile[]{AERIAL_ARTIST, JUGGLER, CONTORTIONIST, FIRE_PERFORMER};
    }

    public static PerformerProfile[] getEntertainers() {
        return new PerformerProfile[]{CLOWN, MAGICIAN, OTHER};
    }
}
