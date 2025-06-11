package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.AICommentResponse;
import com.example.cleopatra.dto.Comment.CommentResponse;
import com.example.cleopatra.dto.CreateCommentWithAIRequest;
import com.example.cleopatra.dto.ImproveCommentRequest;
import com.example.cleopatra.service.CommentService;
import com.example.cleopatra.service.CommentAIService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/posts/{postId}/comments/ai")
@RequiredArgsConstructor
public class CommentAIController {

    private  final  CommentService commentService;
    private final  CommentAIService commentAIService;

    /**
     * 🤖 Создание комментария с помощью AI
     */
    @PostMapping("/create")
    public ResponseEntity<?> createCommentWithAI(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentWithAIRequest request,
            Authentication authentication) {

        try {
            String userEmail = authentication.getName();
            log.info("Создание AI комментария к посту {} пользователем {}", postId, userEmail);

            CommentResponse response = commentService.createCommentWithAI(postId, request, userEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Комментарий успешно создан с помощью AI",
                    "comment", response,
                    "aiGenerated", true
            ));

        } catch (Exception e) {
            log.error("Ошибка при создании AI комментария: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 🔍 Генерация превью комментария (без сохранения)
     */
    @PostMapping("/preview")
    public ResponseEntity<?> generateCommentPreview(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentWithAIRequest request) {

        try {
            log.info("Генерация превью комментария для поста {}", postId);

            AICommentResponse response = commentService.generateCommentPreview(postId, request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "preview", true,
                        "data", response
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", response.getErrorMessage(),
                        "preview", true
                ));
            }

        } catch (Exception e) {
            log.error("Ошибка при генерации превью: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "preview", true
            ));
        }
    }

    /**
     * ✨ Улучшение существующего комментария
     */
    @PostMapping("/improve")
    public ResponseEntity<?> improveComment(
            @PathVariable Long postId,
            @Valid @RequestBody ImproveCommentRequest request) {

        try {
            log.info("Улучшение комментария для поста {}", postId);

            AICommentResponse response = commentService.improveComment(postId, request);

            return ResponseEntity.ok(Map.of(
                    "success", response.isSuccess(),
                    "data", response,
                    "improved", true
            ));

        } catch (Exception e) {
            log.error("Ошибка при улучшении комментария: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "improved", false
            ));
        }
    }

    /**
     * ⚙️ Проверка доступности AI сервиса
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAIStatus() {
        Map<String, Object> status = new HashMap<>();

        boolean isAvailable = commentAIService.isAIServiceAvailable();

        status.put("aiServiceAvailable", isAvailable);
        status.put("status", isAvailable ? "ready" : "unavailable");
        status.put("message", isAvailable
                ? "AI сервис готов для генерации комментариев"
                : "AI сервис недоступен");

        // Добавляем информацию о доступных типах комментариев
        status.put("availableCommentTypes", Map.of(
                "GENERAL", "Общий комментарий",
                "QUESTION", "Вопрос к автору",
                "POSITIVE", "Положительная оценка",
                "CONSTRUCTIVE", "Конструктивная критика",
                "TECHNICAL", "Техническое обсуждение",
                "CREATIVE", "Креативное дополнение"
        ));

        status.put("availableImprovementTypes", Map.of(
                "GENERAL", "Общее улучшение",
                "GRAMMAR", "Исправление грамматики",
                "TONE", "Улучшение тона",
                "CLARITY", "Повышение ясности",
                "POLITENESS", "Повышение вежливости"
        ));

        return ResponseEntity.ok(status);
    }

    /**
     * 📋 Получение примеров промптов
     */
    @GetMapping("/examples")
    public ResponseEntity<?> getPromptExamples() {
        Map<String, Object> examples = new HashMap<>();

        examples.put("general", Map.of(
                "prompt", "выскажи свое мнение о статье",
                "description", "Общий отзыв о посте"
        ));

        examples.put("question", Map.of(
                "prompt", "задай вопрос автору о деталях реализации",
                "description", "Вопрос для уточнения информации"
        ));

        examples.put("positive", Map.of(
                "prompt", "напиши положительный отзыв о полезности материала",
                "description", "Похвала и благодарность автору"
        ));

        examples.put("constructive", Map.of(
                "prompt", "предложи улучшения или дополнения к материалу",
                "description", "Конструктивная критика с предложениями"
        ));

        examples.put("technical", Map.of(
                "prompt", "обсуди технические аспекты решения",
                "description", "Техническое обсуждение реализации"
        ));

        examples.put("creative", Map.of(
                "prompt", "предложи креативное развитие идеи",
                "description", "Творческое дополнение к теме"
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "examples", examples,
                "tips", Map.of(
                        "general", "Будьте конкретны в своих запросах",
                        "length", "Оптимальная длина промпта: 10-100 символов",
                        "context", "Упоминайте специфические детали поста для лучшего результата"
                )
        ));
    }

    /**
     * 🎯 Быстрые шаблоны комментариев
     */
    @GetMapping("/templates")
    public ResponseEntity<?> getCommentTemplates() {
        Map<String, Object> templates = new HashMap<>();

        templates.put("agreement", Map.of(
                "prompt", "согласись с автором и добавь свой опыт",
                "example", "Полностью согласен с вашей точкой зрения! У меня был похожий опыт..."
        ));

        templates.put("disagreement", Map.of(
                "prompt", "вежливо выскажи другое мнение с аргументами",
                "example", "Интересная точка зрения, но я думаю иначе. Позвольте поделиться своим видением..."
        ));

        templates.put("gratitude", Map.of(
                "prompt", "поблагодари за полезную информацию",
                "example", "Спасибо за подробное объяснение! Очень помогло разобраться в теме."
        ));

        templates.put("question", Map.of(
                "prompt", "задай уточняющий вопрос по теме",
                "example", "Очень интересно! А как вы думаете, что будет если...?"
        ));

        templates.put("experience", Map.of(
                "prompt", "поделись своим опытом по теме",
                "example", "В моей практике я сталкивался с похожей ситуацией..."
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "templates", templates,
                "usage", "Используйте эти шаблоны как отправную точку для создания комментариев"
        ));
    }
}