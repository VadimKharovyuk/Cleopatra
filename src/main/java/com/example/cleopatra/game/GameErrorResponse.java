package com.example.cleopatra.game;

import com.example.cleopatra.enums.HintType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ об ошибке в игре")
public class GameErrorResponse {

    @Schema(description = "Код ошибки", example = "GAME_NOT_FOUND")
    private String errorCode;

    @Schema(description = "Сообщение об ошибке", example = "Игровая сессия не найдена")
    private String message;

    @Schema(description = "ID сессии, если применимо", example = "123")
    private Long sessionId;

    @Schema(description = "Детали ошибки для отладки")
    private String details;

    public static GameErrorResponse create(String errorCode, String message) {
        return GameErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    public static GameErrorResponse sessionNotFound(Long sessionId) {
        return GameErrorResponse.builder()
                .errorCode("GAME_SESSION_NOT_FOUND")
                .message("Игровая сессия не найдена")
                .sessionId(sessionId)
                .build();
    }

    public static GameErrorResponse timeExpired() {
        return GameErrorResponse.builder()
                .errorCode("TIME_EXPIRED")
                .message("Время на ответ истекло")
                .build();
    }

    public static GameErrorResponse hintNotAvailable(HintType hintType) {
        return GameErrorResponse.builder()
                .errorCode("HINT_NOT_AVAILABLE")
                .message("Подсказка " + hintType.getDescription() + " недоступна")
                .build();
    }
}
