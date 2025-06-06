package com.example.cleopatra.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TranslationService {

    private final RestTemplate restTemplate;

    @Value("${translation.api.key}")
    private String apiKey;

    @Value("${translation.enabled:false}")
    private boolean translationEnabled;

    @Value("${translation.source.language:en}")
    private String sourceLanguage;

    @Value("${translation.target.language:ru}")
    private String targetLanguage;

    // Кэш для переводов, чтобы не переводить одно и то же несколько раз
    private final Map<String, String> translationCache = new ConcurrentHashMap<>();

    public TranslationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Переводит текст с английского на русский
     */
    public String translateToRussian(String text) {
        if (!translationEnabled || apiKey == null || apiKey.isEmpty()) {
            log.debug("Translation disabled or no API key, returning original text");
            return text;
        }

        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // Проверяем кэш
        String cached = translationCache.get(text);
        if (cached != null) {
            log.debug("Using cached translation for: {}", text);
            return cached;
        }

        try {
            String translated = translateWithGoogleAPI(text, sourceLanguage, targetLanguage);

            // Сохраняем в кэш
            translationCache.put(text, translated);

            log.info("Translated '{}' -> '{}'", text, translated);
            return translated;

        } catch (Exception e) {
            log.warn("Translation failed for text: '{}', error: {}", text, e.getMessage());

            // Пытаемся использовать простой перевод как fallback
            String simpleTranslated = simpleTranslate(text);
            translationCache.put(text, simpleTranslated);

            return simpleTranslated;
        }
    }

    /**
     * Переводит список текстов
     */
    public List<String> translateList(List<String> texts) {
        return texts.stream()
                .map(this::translateToRussian)
                .toList();
    }

    private String translateWithGoogleAPI(String text, String from, String to) {
        // Google Translate API v2 endpoint
        String url = UriComponentsBuilder
                .fromHttpUrl("https://translation.googleapis.com/language/translate/v2")
                .queryParam("key", apiKey)
                .queryParam("q", text)
                .queryParam("source", from)
                .queryParam("target", to)
                .queryParam("format", "text")
                .build()
                .toUriString();

        log.debug("Calling Google Translate API: {}", url.replace(apiKey, "***"));

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                List<Map<String, Object>> translations = (List<Map<String, Object>>) data.get("translations");

                if (!translations.isEmpty()) {
                    String translatedText = (String) translations.get(0).get("translatedText");

                    // Декодируем HTML entities если есть
                    return decodeHtmlEntities(translatedText);
                }
            }

            log.warn("Unexpected response format from Google Translate API");
            return text;

        } catch (Exception e) {
            log.error("Error calling Google Translate API", e);
            throw e;
        }
    }

    /**
     * Простой fallback переводчик для основных слов
     */
    public String simpleTranslate(String text) {
        if (text == null) return text;

        // Базовые переводы для частых слов в викторинах
        String result = text
                .replace("True", "Верно")
                .replace("False", "Неверно")
                .replace("Yes", "Да")
                .replace("No", "Нет")
                .replace("What", "Что")
                .replace("Which", "Какой")
                .replace("Who", "Кто")
                .replace("Where", "Где")
                .replace("When", "Когда")
                .replace("How", "Как")
                .replace("Why", "Почему")
                .replace("Science", "Наука")
                .replace("History", "История")
                .replace("Geography", "География")
                .replace("Sports", "Спорт")
                .replace("Entertainment", "Развлечения")
                .replace("Art", "Искусство")
                .replace("Literature", "Литература")
                .replace("Music", "Музыка")
                .replace("Film", "Кино")
                .replace("Television", "Телевидение")
                .replace("Video Games", "Видеоигры")
                .replace("Animals", "Животные")
                .replace("Nature", "Природа")
                .replace("Politics", "Политика")
                .replace("Mathematics", "Математика")
                .replace("Computer Science", "Информатика")
                .replace("General Knowledge", "Общие знания");

        return result;
    }

    private String decodeHtmlEntities(String text) {
        if (text == null) return text;

        return text
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#x27;", "'")
                .replace("&#x2F;", "/");
    }

    /**
     * Получить статистику кэша переводов
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
                "cacheSize", translationCache.size(),
                "translationEnabled", translationEnabled,
                "apiKeyConfigured", apiKey != null && !apiKey.isEmpty()
        );
    }

    /**
     * Очистить кэш переводов
     */
    public void clearCache() {
        translationCache.clear();
        log.info("Translation cache cleared");
    }
}