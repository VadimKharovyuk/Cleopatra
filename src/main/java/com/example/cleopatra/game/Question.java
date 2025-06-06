package com.example.cleopatra.game;

import com.example.cleopatra.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String questionText;
    private String correctAnswer;
    private List<String> incorrectAnswers;
    private List<String> allAnswers;
    private int correctAnswerIndex;
    private String category;
    private DifficultyLevel difficulty;

    public void applyFiftyFifty() {
        if (allAnswers.size() != 4) return;

        List<String> newAnswers = new ArrayList<>();
        newAnswers.add(correctAnswer);

        List<String> incorrectCopy = new ArrayList<>(incorrectAnswers);
        Collections.shuffle(incorrectCopy);
        newAnswers.add(incorrectCopy.get(0));

        Collections.shuffle(newAnswers);
        this.allAnswers = newAnswers;
        this.correctAnswerIndex = newAnswers.indexOf(correctAnswer);
    }
}