package com.example.cleopatra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentWithAIRequest {

    /**
     * Промпт пользователя для генерации комментария
     * Например: "напиши положительный отзыв", "выскажи сомнения", "задай вопрос автору"
     */
    @NotBlank(message = "Промпт не может быть пустым")
    @Size(min = 3, max = 500, message = "Промпт должен содержать от 3 до 500 символов")
    private String prompt;

    /**
     * Опционально: дополнительный контекст от пользователя
     */
    @Size(max = 200, message = "Дополнительный контекст не может превышать 200 символов")
    private String additionalContext;

    /**
     * Тип комментария для более точной генерации
     */
    private CommentType commentType = CommentType.GENERAL;

    public enum CommentType {
        GENERAL("Общий комментарий"),
        QUESTION("Вопрос к автору"),
        POSITIVE("Положительная оценка"),
        CONSTRUCTIVE("Конструктивная критика"),
        TECHNICAL("Техническое обсуждение"),
        CREATIVE("Креативное дополнение");

        private final String description;

        CommentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
