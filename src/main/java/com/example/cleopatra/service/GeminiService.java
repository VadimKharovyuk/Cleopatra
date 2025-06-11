package com.example.cleopatra.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.action}")
    private String action;

    private boolean isApiKeyValid = false;
    private String fullEndpoint;

    @PostConstruct
    public void init() {
        logger.info("Инициализация GeminiService...");
        buildEndpoint();
        validateApiKey();
        testApiConnection();
    }

    private void buildEndpoint() {
        fullEndpoint = String.format("%s/%s:%s", baseUrl, model, action);
        logger.info("🔧 Построен endpoint: {}", fullEndpoint);
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("❌ API ключ не настроен!");
            return;
        }

        if (apiKey.equals("${gemini.api.key}") || apiKey.equals("${GEMINI_API_KEY}")) {
            logger.error("❌ API ключ не загружен из конфигурации! Текущее значение: {}", apiKey);
            return;
        }

        if (apiKey.length() < 30) {
            logger.warn("⚠️ API ключ кажется слишком коротким. Длина: {} символов", apiKey.length());
        }

        if (!apiKey.startsWith("AIza")) {
            logger.warn("⚠️ API ключ не соответствует формату Google API (должен начинаться с 'AIza')");
        }

        logger.info("✅ API ключ загружен успешно. Длина: {} символов", apiKey.length());
        logger.info("✅ Модель: {}", model);
        logger.info("✅ Endpoint: {}", fullEndpoint);
        isApiKeyValid = true;
    }

    private void testApiConnection() {
        if (!isApiKeyValid) {
            logger.error("❌ Пропуск тестирования соединения - API ключ недействителен");
            return;
        }

        try {
            logger.info("🔄 Тестирование соединения с Gemini API ({})...", model);
            String testResponse = generateContent("Привет! Ответь одним словом.");

            if (testResponse != null && !testResponse.contains("Ошибка")) {
                logger.info("✅ Подключение к Gemini API успешно! Модель: {} | Ответ: {}",
                        model, testResponse.length() > 50 ? testResponse.substring(0, 50) + "..." : testResponse);
            } else {
                logger.error("❌ Тест соединения не прошел. Ответ: {}", testResponse);
            }

        } catch (Exception e) {
            logger.error("❌ Ошибка при тестировании соединения: {}", e.getMessage());

            // Дополнительная диагностика
            if (e.getMessage().contains("401")) {
                logger.error("🔑 Ошибка авторизации - проверьте правильность API ключа");
            } else if (e.getMessage().contains("403")) {
                logger.error("🚫 Доступ запрещен - возможно API ключ отключен или превышена квота");
            } else if (e.getMessage().contains("404")) {
                logger.error("🔍 Endpoint не найден - проверьте модель: {}", model);
            } else if (e.getMessage().contains("timeout")) {
                logger.error("⏰ Превышено время ожидания - проверьте интернет соединение");
            }
        }
    }

    public boolean isServiceReady() {
        return isApiKeyValid;
    }

    public String getServiceStatus() {
        if (!isApiKeyValid) {
            return "❌ Сервис не готов - проблемы с API ключом";
        }
        return String.format("✅ Сервис готов к работе (модель: %s)", model);
    }

    public String getModelInfo() {
        return String.format("Модель: %s | Endpoint: %s", model, fullEndpoint);
    }

    public String generateContent(String prompt) throws Exception {
        if (!isApiKeyValid) {
            throw new IllegalStateException("API ключ не настроен или недействителен. Проверьте конфигурацию.");
        }

        // Формируем полный URL с API ключом
        String requestUrl = fullEndpoint + "?key=" + apiKey;

        logger.debug("Отправка запроса к Gemini API с промптом: {}",
                prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);

        // Создаем JSON структуру запроса
        JsonObject content = new JsonObject();
        JsonObject parts = new JsonObject();
        parts.addProperty("text", prompt);
        JsonArray partsArray = new JsonArray();
        partsArray.add(parts);

        JsonObject contents = new JsonObject();
        contents.add("parts", partsArray);

        JsonArray contentsArray = new JsonArray();
        contentsArray.add(contents);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contentsArray);

        // Создаем HTTP запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        // Отправляем запрос
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.debug("Получен ответ от API. Статус: {}", response.statusCode());

        if (response.statusCode() == 200) {
            String result = parseResponse(response.body());
            logger.debug("Успешно обработан ответ. Длина: {} символов", result.length());
            return result;
        } else {
            String errorMsg = String.format("API запрос не удался. Статус: %d, Ответ: %s",
                    response.statusCode(), response.body());
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    private String parseResponse(String jsonResponse) {
        try {
            Gson gson = new Gson();
            JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);

            JsonArray candidates = responseObj.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject content = candidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    JsonObject part = parts.get(0).getAsJsonObject();
                    return part.get("text").getAsString();
                }
            }

            return "Не удалось получить ответ от Gemini API";
        } catch (Exception e) {
            logger.error("Ошибка при парсинге ответа: {}", e.getMessage());
            return "Ошибка парсинга ответа: " + e.getMessage();
        }
    }
}