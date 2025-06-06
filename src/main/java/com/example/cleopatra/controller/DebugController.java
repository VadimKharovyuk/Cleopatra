package com.example.cleopatra.controller;

import com.example.cleopatra.enums.DifficultyLevel;
import com.example.cleopatra.game.LazyOpenTDBQuestionProvider;
import com.example.cleopatra.game.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final LazyOpenTDBQuestionProvider questionProvider;

    @GetMapping("/load-questions")
    public Map<String, Object> loadQuestions(
            @RequestParam(defaultValue = "EASY") String difficulty,
            @RequestParam(defaultValue = "1") int amount) {

        Map<String, Object> response = new HashMap<>();

        try {
            DifficultyLevel level = DifficultyLevel.valueOf(difficulty.toUpperCase());
            List<Question> questions = questionProvider.getQuestions(level, amount);

            response.put("success", true);
            response.put("difficulty", difficulty);
            response.put("requested", amount);
            response.put("loaded", questions.size());
            response.put("questions", questions);
            response.put("cacheStats", questionProvider.getCacheStats());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @GetMapping("/test-single")
    public Map<String, Object> testSingle() {
        Map<String, Object> response = new HashMap<>();

        try {
            Question question = questionProvider.getQuestion(DifficultyLevel.EASY);
            response.put("success", true);
            response.put("question", question);
            response.put("cacheStats", questionProvider.getCacheStats());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }
}