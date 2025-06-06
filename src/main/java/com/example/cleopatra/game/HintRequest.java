package com.example.cleopatra.game;


import com.example.cleopatra.enums.HintType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос для использования подсказки")
public class HintRequest {

    @NotNull(message = "ID сессии обязателен")
    @Schema(description = "ID игровой сессии", example = "123")
    private Long sessionId;

    @NotNull(message = "Тип подсказки обязателен")
    @Schema(description = "Тип подсказки", example = "FIFTY_FIFTY")
    private HintType hintType;
}