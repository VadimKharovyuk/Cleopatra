package com.example.cleopatra.game;


import com.example.cleopatra.enums.GameStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Подробная информация об игре")
public class GameDetailDto {

    @Schema(description = "ID игровой сессии")
    private Long sessionId;

    @Schema(description = "Статус игры")
    private GameStatus gameStatus;

    @Schema(description = "Итоговый счет")
    private Integer totalScore;

    @Schema(description = "Количество отвеченных вопросов")
    private Integer questionsAnswered;

    @Schema(description = "Время начала игры")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @Schema(description = "Время окончания игры")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;

    @Schema(description = "Детали всех ответов")
    private List<AnswerDetailDto> answers;

    @Schema(description = "Использованные подсказки")
    private String[] hintsUsed;

    public Long getDurationMinutes() {
        if (startedAt == null || finishedAt == null) return null;
        return java.time.Duration.between(startedAt, finishedAt).toMinutes();
    }

    public Float getAccuracyPercentage() {
        if (answers == null || answers.isEmpty()) return 0f;
        long correctCount = answers.stream().mapToLong(a -> a.getIsCorrect() ? 1 : 0).sum();
        return (float) correctCount / answers.size() * 100;
    }
}
