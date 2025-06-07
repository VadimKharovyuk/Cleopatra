package com.example.cleopatra.game;

import com.example.cleopatra.enums.HintType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос для отправки ответа на вопрос")
public class AnswerRequest {

    @NotNull(message = "ID сессии обязателен")
    private Long sessionId;

    @NotBlank(message = "Ответ не может быть пустым")
    private String playerAnswer;
}

