package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum ForumType {

    GENERAL("🗨️", "Общие вопросы", "Общие обсуждения и вопросы, не относящиеся к конкретной категории"),

    JOBS("💼", "Работа", "Поиск работы, вакансии, предложения от работодателей"),

    COSTUMES("👗", "Костюмы и реквизит", "Обсуждение костюмов, реквизита, аксессуаров для выступлений"),

    CONTRACTS("📋", "Контракты", "Юридические вопросы, договоры, правовые консультации"),

    LOOKING_FOR_PARTNER("🤝", "Поиск партнеров", "Поиск напарников, команды, творческого коллектива"),

    BLACKLIST("⚠️", "Черный список", "Предупреждения о недобросовестных заказчиках и работодателях"),

    EQUIPMENT("🎤", "Оборудование", "Техника, оборудование, звук, свет для выступлений"),

    TRAINING("🎓", "Обучение", "Мастер-классы, курсы, образовательные программы для артистов");

    private final String emoji;
    private final String displayName;
    private final String description;

    ForumType(String emoji, String displayName, String description) {
        this.emoji = emoji;
        this.displayName = displayName;
        this.description = description;
    }
}