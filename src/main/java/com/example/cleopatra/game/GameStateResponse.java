package com.example.cleopatra.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о текущем состоянии игры")
public class GameStateResponse {

    @Schema(description = "ID игровой сессии")
    private Long sessionId;

    @Schema(description = "Статус игры")
    private String gameStatus;

    @Schema(description = "Номер текущего вопроса")
    private Integer currentQuestion;

    @Schema(description = "Общий счет")
    private Integer totalScore;

    @Schema(description = "Доступные подсказки")
    private String[] availableHints;

    @Schema(description = "Время начала игры")
    private String startedAt;

    @Schema(description = "Оставшееся время на текущий вопрос (секунды)")
    private Integer timeRemaining;
}
