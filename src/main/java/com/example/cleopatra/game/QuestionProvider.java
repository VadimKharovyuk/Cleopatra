package com.example.cleopatra.game;

import com.example.cleopatra.enums.DifficultyLevel;
import java.util.List;

public interface QuestionProvider {
    Question getQuestion(DifficultyLevel difficulty);
    List<Question> getQuestions(DifficultyLevel difficulty, int amount);
    boolean isAvailable();
}