package com.example.cleopatra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICommentResponse {

    /**
     * Сгенерированный комментарий
     */
    private String generatedComment;

    /**
     * Промпт, который использовался для генерации
     */
    private String usedPrompt;

    /**
     * Время генерации в миллисекундах
     */
    private Long generationTimeMs;

    /**
     * Успешность генерации
     */
    private boolean success;

    /**
     * Сообщение об ошибке (если есть)
     */
    private String errorMessage;

    /**
     * Дополнительная информация
     */
    private String additionalInfo;
}