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
@Schema(description = "Настройки игры")
public class GameSettings {

    @Schema(description = "Ограничение времени на вопрос в секундах", example = "30")
    private Integer timeLimit;

    @Schema(description = "Сложность начальных вопросов", example = "EASY")
    private String startDifficulty;

    @Schema(description = "Включить звуковые эффекты", example = "true")
    private Boolean soundEnabled;

    public static GameSettings getDefault() {
        return GameSettings.builder()
                .timeLimit(30)
                .startDifficulty("EASY")
                .soundEnabled(true)
                .build();
    }
}
