package com.example.cleopatra.game;

import com.example.cleopatra.enums.DifficultyLevel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Статистика игрока по всем играм")
public class PlayerGameStatsDto {

    @Schema(description = "Общее количество игр")
    private Integer totalGames;

    @Schema(description = "Завершенные игры")
    private Integer completedGames;

    @Schema(description = "Проваленные игры")
    private Integer failedGames;

    @Schema(description = "Покинутые игры")
    private Integer abandonedGames;

    @Schema(description = "Лучший результат")
    private Integer bestScore;

    @Schema(description = "Максимальный достигнутый вопрос")
    private Integer maxQuestionReached;

    @Schema(description = "Общее количество ответов")
    private Integer totalAnswers;

    @Schema(description = "Правильные ответы")
    private Integer correctAnswers;

    @Schema(description = "Процент точности")
    private Float accuracyPercentage;

    @Schema(description = "Среднее время на ответ в секундах")
    private Float averageAnswerTime;

    @Schema(description = "Общее время в игре в минутах")
    private Long totalPlayTime;

    @Schema(description = "Любимая сложность вопросов")
    private DifficultyLevel favoriteDifficulty;

    @Schema(description = "Последняя игра")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPlayedAt;

    public Float getCompletionRate() {
        if (totalGames == null || totalGames == 0) return 0f;
        return (completedGames != null ? completedGames.floatValue() : 0f) / totalGames * 100;
    }

    public Float getWinRate() {
        if (totalGames == null || totalGames == 0) return 0f;
        int successfulGames = (completedGames != null ? completedGames : 0) +
                (failedGames != null ? failedGames : 0); // failed тоже считается попыткой
        return (completedGames != null ? completedGames.floatValue() : 0f) / totalGames * 100;
    }

    public String getPlayerLevel() {
        if (bestScore == null || bestScore == 0) return "Новичок";
        if (bestScore < 1000) return "Начинающий";
        if (bestScore < 5000) return "Любитель";
        if (bestScore < 15000) return "Опытный";
        if (bestScore < 50000) return "Эксперт";
        return "Мастер";
    }
}