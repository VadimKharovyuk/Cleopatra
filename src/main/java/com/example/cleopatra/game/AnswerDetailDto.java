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
@Schema(description = "Детали ответа на вопрос")
public class AnswerDetailDto {

    @Schema(description = "Номер вопроса", example = "15")
    private Integer questionNumber;

    @Schema(description = "Текст вопроса")
    private String questionText;

    @Schema(description = "Правильный ответ")
    private String correctAnswer;

    @Schema(description = "Ответ игрока")
    private String playerAnswer;

    @Schema(description = "Правильность ответа")
    private Boolean isCorrect;

    @Schema(description = "Заработанные очки", example = "500")
    private Integer pointsEarned;

    @Schema(description = "Уровень сложности")
    private DifficultyLevel difficulty;

    @Schema(description = "Время на ответ в секундах", example = "25")
    private Integer timeTaken;

    @Schema(description = "Время ответа")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime answeredAt;
}
