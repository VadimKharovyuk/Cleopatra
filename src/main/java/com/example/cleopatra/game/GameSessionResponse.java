package com.example.cleopatra.game;

import com.example.cleopatra.enums.HintType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionResponse {

    // Основная информация о сессии
    private Long sessionId;
    private Integer currentQuestion;
    private Integer totalScore;
    private Integer timeLimit;

    // Текущий вопрос
    private Question question;

    // Доступные подсказки
    private String[] availableHints;

    // Результат последнего действия
    private Boolean lastAnswerCorrect;
    private Integer pointsEarned;
    private String correctAnswer;
    private HintType hintUsed;
    private Boolean questionSkipped;
    private Boolean timeExpired;

    // Завершение игры
    private Boolean gameOver;
    private Boolean gameCompleted;
    private Integer finalScore;
    private Integer questionsAnswered;

    // Конструктор для простых ответов
    public static GameSessionResponse success(Long sessionId, String message) {
        return GameSessionResponse.builder()
                .sessionId(sessionId)
                .build();
    }

    // Конструктор для ошибок
    public static GameSessionResponse error(String message) {
        return new GameSessionResponse();
    }
}