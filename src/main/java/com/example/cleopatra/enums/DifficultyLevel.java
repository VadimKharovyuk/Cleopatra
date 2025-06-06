package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum DifficultyLevel {
    EASY(1, 20, "easy", 100),
    MEDIUM(21, 40, "medium", 200),
    HARD(41, 60, "hard", 500),
    EXPERT(61, 80, "hard", 1000),
    GENIUS(81, 100, "hard", 2000);

    private final int minQuestion;
    private final int maxQuestion;
    private final String apiDifficulty;
    private final int pointsPerQuestion;

    DifficultyLevel(int minQuestion, int maxQuestion, String apiDifficulty, int pointsPerQuestion) {
        this.minQuestion = minQuestion;
        this.maxQuestion = maxQuestion;
        this.apiDifficulty = apiDifficulty;
        this.pointsPerQuestion = pointsPerQuestion;
    }
}
