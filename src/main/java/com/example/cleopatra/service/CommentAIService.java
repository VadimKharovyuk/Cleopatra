package com.example.cleopatra.service;

import com.example.cleopatra.model.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CommentAIService {

    private static final Logger log = LoggerFactory.getLogger(CommentAIService.class);

    @Autowired
    private GeminiService geminiService;

    /**
     * Генерирует комментарий на основе промпта пользователя и контекста поста
     */
    public String generateComment(String userPrompt, Post post) {
        try {
            String enhancedPrompt = buildContextualPrompt(userPrompt, post);
            log.debug("Генерация комментария с промптом: {}", enhancedPrompt);

            String generatedComment = geminiService.generateContent(enhancedPrompt);

            // Очищаем и форматируем ответ
            String cleanedComment = cleanGeneratedComment(generatedComment);

            log.info("Комментарий успешно сгенерирован. Длина: {} символов", cleanedComment.length());
            return cleanedComment;

        } catch (Exception e) {
            log.error("Ошибка при генерации комментария: {}", e.getMessage());
            throw new RuntimeException("Не удалось сгенерировать комментарий: " + e.getMessage());
        }
    }

    /**
     * Улучшает существующий комментарий пользователя
     */
    public String improveComment(String originalComment, Post post) {
        try {
            String improvementPrompt = buildImprovementPrompt(originalComment, post);
            log.debug("Улучшение комментария с промптом: {}", improvementPrompt);

            String improvedComment = geminiService.generateContent(improvementPrompt);
            String cleanedComment = cleanGeneratedComment(improvedComment);

            log.info("Комментарий успешно улучшен");
            return cleanedComment;

        } catch (Exception e) {
            log.error("Ошибка при улучшении комментария: {}", e.getMessage());
            // Возвращаем оригинальный комментарий в случае ошибки
            return originalComment;
        }
    }

    /**
     * Создает контекстуальный промпт на основе поста и запроса пользователя
     */
    private String buildContextualPrompt(String userPrompt, Post post) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("Ты помощник для написания комментариев к постам в социальной сети. ");
        promptBuilder.append("Напиши комментарий, который будет:\n");
        promptBuilder.append("- Конструктивным и вежливым\n");
        promptBuilder.append("- Относящимся к теме поста\n");
        promptBuilder.append("- Длиной не более 200 символов\n");
        promptBuilder.append("- На русском языке\n\n");

        // Добавляем контекст поста
        promptBuilder.append("Контекст поста:\n");

        if (post.getContent() != null && !post.getContent().isEmpty()) {
            // Ограничиваем длину контента для промпта
            String shortContent = post.getContent().length() > 300
                    ? post.getContent().substring(0, 300) + "..."
                    : post.getContent();
            promptBuilder.append("Содержание: ").append(shortContent).append("\n");
        }

        promptBuilder.append("\nЗапрос пользователя: ").append(userPrompt).append("\n\n");
        promptBuilder.append("Напиши комментарий:");

        return promptBuilder.toString();
    }

    /**
     * Создает промпт для улучшения существующего комментария
     */
    private String buildImprovementPrompt(String originalComment, Post post) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("Улучши этот комментарий к посту, сделав его более:\n");
        promptBuilder.append("- Конструктивным и полезным\n");
        promptBuilder.append("- Вежливым и дружелюбным\n");
        promptBuilder.append("- Грамматически правильным\n");
        promptBuilder.append("- Связанным с темой поста\n\n");

        promptBuilder.append("Оригинальный комментарий: ").append(originalComment).append("\n\n");
        promptBuilder.append("Улучшенный комментарий:");

        return promptBuilder.toString();
    }

    /**
     * Очищает сгенерированный комментарий от лишних элементов
     */
    private String cleanGeneratedComment(String generatedComment) {
        if (generatedComment == null || generatedComment.trim().isEmpty()) {
            return "Спасибо за интересный пост!";
        }

        String cleaned = generatedComment.trim();

        // Убираем типичные префиксы от AI
        String[] prefixesToRemove = {
                "Комментарий:", "Ответ:", "Мой комментарий:",
                "Вот комментарий:", "Комментарий к посту:"
        };

        for (String prefix : prefixesToRemove) {
            if (cleaned.toLowerCase().startsWith(prefix.toLowerCase())) {
                cleaned = cleaned.substring(prefix.length()).trim();
            }
        }

        // Убираем кавычки в начале и конце
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        // Ограничиваем длину
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 197) + "...";
        }

        // Проверяем минимальную длину
        if (cleaned.length() < 3) {
            return "Интересный пост, спасибо за публикацию!";
        }

        return cleaned;
    }

    /**
     * Проверяет, доступен ли AI сервис
     */
    public boolean isAIServiceAvailable() {
        return geminiService.isServiceReady();
    }
}