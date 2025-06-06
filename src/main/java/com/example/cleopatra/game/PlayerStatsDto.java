package com.example.cleopatra.game;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatsDto {

    // Основная информация о пользователе
    private Long userId;
    private String username;
    private String avatarUrl;
    private Integer rank;
    private Boolean isSubscribed;

    // Игровая статистика
    private Integer bestScore;
    private Integer bestQuestionReached;
    private Integer totalGamesPlayed;
    private Integer gamesCompleted;
    private Integer gamesFailed;

    // Статистика ответов
    private Integer totalAnswers;
    private Integer correctAnswers;
    private Float accuracyPercentage;

    // Временные метки
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPlayed;

    // Вычисляемые поля
    public Float getCompletionRate() {
        if (totalGamesPlayed == null || totalGamesPlayed == 0) {
            return 0f;
        }
        return (gamesCompleted != null ? gamesCompleted.floatValue() : 0f) / totalGamesPlayed * 100;
    }

    public String getPerformanceLevel() {
        if (bestScore == null || bestScore == 0) {
            return "Новичок";
        } else if (bestScore < 1000) {
            return "Начинающий";
        } else if (bestScore < 5000) {
            return "Любитель";
        } else if (bestScore < 15000) {
            return "Опытный";
        } else if (bestScore < 50000) {
            return "Эксперт";
        } else {
            return "Мастер";
        }
    }
}