package com.example.cleopatra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

@Data
public class ImproveCommentRequest {

    /**
     * Оригинальный комментарий для улучшения
     */
    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(min = 3, max = 1000, message = "Комментарий должен содержать от 3 до 1000 символов")
    private String originalComment;

    /**
     * Тип улучшения
     */
    private ImprovementType improvementType = ImprovementType.GENERAL;

    @Getter
    public enum ImprovementType {
        GENERAL("Общее улучшение"),
        GRAMMAR("Исправление грамматики"),
        TONE("Улучшение тона"),
        CLARITY("Повышение ясности"),
        POLITENESS("Повышение вежливости");

        private final String description;

        ImprovementType(String description) {
            this.description = description;
        }

    }
}