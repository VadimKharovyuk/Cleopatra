package com.example.cleopatra.game;


import com.example.cleopatra.enums.DifficultyLevel;
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
@Schema(description = "Краткая информация об игре в истории")
public class GameHistoryDto {

    @Schema(description = "ID игровой сессии", example = "123")
    private Long sessionId;

    @Schema(description = "Статус игры")
    private GameStatus gameStatus;

    @Schema(description = "Итоговый счет", example = "15000")
    private Integer totalScore;

    @Schema(description = "Количество отвеченных вопросов", example = "25")
    private Integer questionsAnswered;

    @Schema(description = "Время начала игры")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @Schema(description = "Время окончания игры")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;

    @Schema(description = "Продолжительность игры в минутах", example = "45")
    private Long duration;

    @Schema(description = "Количество использованных подсказок", example = "2")
    private Integer hintsUsed;

    public String getStatusDescription() {
        switch (gameStatus) {
            case COMPLETED:
                return "Игра завершена успешно";
            case FAILED:
                return "Неправильный ответ";
            case ABANDONED:
                return "Игра покинута";
            case IN_PROGRESS:
                return "В процессе";
            default:
                return "Неизвестный статус";
        }
    }

    public String getPerformanceRating() {
        if (totalScore == null || totalScore == 0) return "F";
        if (totalScore < 1000) return "D";
        if (totalScore < 5000) return "C";
        if (totalScore < 15000) return "B";
        if (totalScore < 50000) return "A";
        return "A+";
    }
}
