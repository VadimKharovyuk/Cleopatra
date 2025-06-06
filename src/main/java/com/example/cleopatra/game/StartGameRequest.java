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
@Schema(description = "Запрос для начала новой игры")
public class StartGameRequest {

    @Schema(description = "Настройки игры (опционально)")
    private GameSettings settings;
}