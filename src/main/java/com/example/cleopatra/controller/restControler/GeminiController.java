package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/gemini")
@CrossOrigin(origins = "*") // Для тестирования в Postman
public class GeminiController {

    @Autowired
    private GeminiService geminiService;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    // 1. Основной endpoint для генерации контента
    @PostMapping("/generate")
    public ResponseEntity<?> generateContent(@RequestBody Map<String, String> request) {
        try {
            // Проверяем готовность сервиса
            if (!geminiService.isServiceReady()) {
                return ResponseEntity.status(503)
                        .body(createErrorResponse("Сервис не готов к работе",
                                geminiService.getServiceStatus()));
            }

            String prompt = request.get("prompt");

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Поле 'prompt' обязательно и не может быть пустым", null));
            }

            long startTime = System.currentTimeMillis();
            String response = geminiService.generateContent(prompt);
            long endTime = System.currentTimeMillis();

            return ResponseEntity.ok(createSuccessResponse(prompt, response, endTime - startTime));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Ошибка при генерации контента: " + e.getMessage(), null));
        }
    }

    // 2. Быстрый тест с предустановленными промптами
    @GetMapping("/quick-test")
    public ResponseEntity<?> quickTest(@RequestParam(defaultValue = "greeting") String type) {
        try {
            String prompt = getTestPrompt(type);

            if (!geminiService.isServiceReady()) {
                return ResponseEntity.status(503)
                        .body(createErrorResponse("Сервис не готов к работе",
                                geminiService.getServiceStatus()));
            }

            long startTime = System.currentTimeMillis();
            String response = geminiService.generateContent(prompt);
            long endTime = System.currentTimeMillis();

            Map<String, Object> result = createSuccessResponse(prompt, response, endTime - startTime);
            result.put("testType", type);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Ошибка при быстром тесте: " + e.getMessage(), null));
        }
    }

    // 3. Проверка здоровья сервиса
    @GetMapping("/health")
    public ResponseEntity<?> getHealth() {
        boolean isReady = geminiService.isServiceReady();
        String status = geminiService.getServiceStatus();

        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", isReady ? "healthy" : "unhealthy");
        healthInfo.put("message", status);
        healthInfo.put("ready", isReady);
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("model", model);
        healthInfo.put("modelInfo", geminiService.getModelInfo());

        if (isReady) {
            return ResponseEntity.ok(healthInfo);
        } else {
            return ResponseEntity.status(503).body(healthInfo);
        }
    }

    // 4. Информация о конфигурации
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("model", model);
        config.put("baseUrl", baseUrl);
        config.put("serviceReady", geminiService.isServiceReady());
        config.put("modelInfo", geminiService.getModelInfo());
        config.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(config);
    }

    // 5. Статус и доступные endpoints
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Gemini AI Service");
        status.put("status", "active");
        status.put("model", model);
        status.put("ready", geminiService.isServiceReady());
        status.put("timestamp", LocalDateTime.now());

        // Список доступных endpoints для Postman
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("POST /api/gemini/generate", "Генерация контента - основной endpoint");
        endpoints.put("GET /api/gemini/quick-test", "Быстрый тест (параметр: type=greeting|joke|story|creative)");
        endpoints.put("GET /api/gemini/health", "Проверка здоровья сервиса");
        endpoints.put("GET /api/gemini/config", "Информация о конфигурации");
        endpoints.put("GET /api/gemini/status", "Статус сервиса и список endpoints");
        endpoints.put("GET /api/gemini/examples", "Примеры запросов для Postman");

        status.put("endpoints", endpoints);

        return ResponseEntity.ok(status);
    }

    // 6. Примеры запросов для Postman
    @GetMapping("/examples")
    public ResponseEntity<?> getExamples() {
        Map<String, Object> examples = new HashMap<>();

        // Примеры POST запросов
        Map<String, Object> postExamples = new HashMap<>();
        postExamples.put("simple", Map.of(
                "url", "POST /api/gemini/generate",
                "body", Map.of("prompt", "Привет! Как дела?")
        ));
        postExamples.put("creative", Map.of(
                "url", "POST /api/gemini/generate",
                "body", Map.of("prompt", "Напиши короткий рассказ о роботе, который научился мечтать")
        ));
        postExamples.put("technical", Map.of(
                "url", "POST /api/gemini/generate",
                "body", Map.of("prompt", "Объясни принцип работы нейронных сетей простыми словами")
        ));

        // Примеры GET запросов
        Map<String, String> getExamples = new HashMap<>();
        getExamples.put("greeting", "GET /api/gemini/quick-test?type=greeting");
        getExamples.put("joke", "GET /api/gemini/quick-test?type=joke");
        getExamples.put("story", "GET /api/gemini/quick-test?type=story");
        getExamples.put("creative", "GET /api/gemini/quick-test?type=creative");

        examples.put("postExamples", postExamples);
        examples.put("getExamples", getExamples);
        examples.put("headers", Map.of("Content-Type", "application/json"));

        return ResponseEntity.ok(examples);
    }

    // Вспомогательные методы
    private String getTestPrompt(String type) {
        return switch (type.toLowerCase()) {
            case "greeting" -> "Привет! Напиши дружелюбное приветствие.";
            case "joke" -> "Расскажи хороший анекдот.";
            case "story" -> "Напиши очень короткий рассказ о приключениях кота.";
            case "creative" -> "Придумай креативную идею для мобильного приложения.";
            case "tech" -> "Объясни что такое искусственный интеллект в двух предложениях.";
            default -> "Привет! Ответь одним предложением.";
        };
    }

    private Map<String, Object> createSuccessResponse(String prompt, String response, long responseTime) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("prompt", prompt);
        result.put("response", response);
        result.put("responseTimeMs", responseTime);
        result.put("model", model);
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    private Map<String, Object> createErrorResponse(String error, String details) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", error);
        if (details != null) {
            result.put("details", details);
        }
        result.put("timestamp", LocalDateTime.now());
        return result;
    }
}