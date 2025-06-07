package com.example.cleopatra.game;
import com.example.cleopatra.enums.DifficultyLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LazyOpenTDBQuestionProvider implements QuestionProvider {

    private final RestTemplate restTemplate;
    private final FallbackQuestionProvider fallbackProvider;
    private static final String BASE_URL = "https://opentdb.com/api.php";

    // Небольшой кэш для текущего уровня - максимум 5 вопросов
    private final Map<DifficultyLevel, Queue<Question>> questionCache = new ConcurrentHashMap<>();

    // Время последнего запроса к API для rate limiting
    private volatile LocalDateTime lastApiCall = LocalDateTime.now().minusSeconds(15);

    // Минимальный интервал между запросами (в секундах)
    private static final int MIN_REQUEST_INTERVAL = 10;

    private final TranslationService translationService;

    public LazyOpenTDBQuestionProvider(RestTemplate restTemplate,
                                       FallbackQuestionProvider fallbackProvider,
                                       TranslationService translationService) {
        this.restTemplate = restTemplate;
        this.fallbackProvider = fallbackProvider;
        this.translationService = translationService;
        log.info("Created LazyOpenTDBQuestionProvider - questions will be loaded on demand");

        // Инициализируем пустые очереди
        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
            questionCache.put(difficulty, new LinkedList<>());
        }
    }

    @Override
    public Question getQuestion(DifficultyLevel difficulty) {
        // Сначала пытаемся получить из кэша
        Question cachedQuestion = getQuestionFromCache(difficulty);
        if (cachedQuestion != null) {
            log.debug("Returning cached question for {} difficulty", difficulty);
            return cachedQuestion;
        }

        // Если в кэше нет, пытаемся загрузить ОДИН вопрос
        Question freshQuestion = loadSingleQuestion(difficulty);
        if (freshQuestion != null) {
            log.info("Loaded fresh question for {} difficulty", difficulty);
            return freshQuestion;
        }

        // Если не удалось загрузить, используем fallback
        log.warn("Using fallback question for {} difficulty", difficulty);
        return fallbackProvider.getQuestion(difficulty);
    }

    @Override
    public List<Question> getQuestions(DifficultyLevel difficulty, int amount) {
        List<Question> result = new ArrayList<>();

        // Получаем по одному вопросу
        for (int i = 0; i < amount; i++) {
            Question question = getQuestion(difficulty);
            if (question != null) {
                result.add(question);

                // Небольшая пауза между получениями вопросов если нужно больше одного
                if (i < amount - 1) {
                    try {
                        Thread.sleep(1000); // 1 секунда между вопросами
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean isAvailable() {
        try {
            // Проверяем только если прошло достаточно времени
            if (shouldThrottleRequest()) {
                return true; // Предполагаем что доступен
            }

            String testUrl = BASE_URL + "?amount=1&type=multiple&difficulty=easy";
            OpenTDBResponse response = restTemplate.getForObject(testUrl, OpenTDBResponse.class);
            updateLastApiCall();
            return response != null && response.getResponseCode() == 0;
        } catch (Exception e) {
            log.debug("OpenTDB availability check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Предварительно загружаем несколько вопросов для указанного уровня
     * Используется когда игрок приближается к новому уровню сложности
     */
    public void preloadQuestionsForLevel(DifficultyLevel difficulty) {
        log.info("Preloading questions for {} difficulty", difficulty);

        // Загружаем 2-3 вопроса заранее
        new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    if (shouldThrottleRequest()) {
                        long waitTime = calculateWaitTime();
                        log.info("Waiting {} seconds before preloading question {}", waitTime, i + 1);
                        Thread.sleep(waitTime * 1000);
                    }

                    Question question = loadSingleQuestion(difficulty);
                    if (question != null) {
                        addQuestionToCache(difficulty, question);
                        log.info("Preloaded question {} for {} difficulty", i + 1, difficulty);
                    } else {
                        log.warn("Failed to preload question {} for {} difficulty", i + 1, difficulty);
                        break; // Прекращаем если не удалось загрузить
                    }

                    // Пауза между загрузками
                    Thread.sleep(MIN_REQUEST_INTERVAL * 1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Preloading interrupted for {} difficulty", difficulty);
            } catch (Exception e) {
                log.error("Error during preloading for " + difficulty, e);
            }
        }, "preload-" + difficulty.name().toLowerCase()).start();
    }

    // ============ Приватные методы ============

    private Question getQuestionFromCache(DifficultyLevel difficulty) {
        Queue<Question> cache = questionCache.get(difficulty);
        if (cache != null && !cache.isEmpty()) {
            synchronized (cache) {
                return cache.poll();
            }
        }
        return null;
    }

    private void addQuestionToCache(DifficultyLevel difficulty, Question question) {
        Queue<Question> cache = questionCache.get(difficulty);
        if (cache != null) {
            synchronized (cache) {
                // Ограничиваем размер кэша
                if (cache.size() >= 5) {
                    cache.poll(); // Удаляем старый вопрос
                }
                cache.offer(question);
                log.debug("Added question to {} cache. Cache size: {}", difficulty, cache.size());
            }
        }
    }

    private Question loadSingleQuestion(DifficultyLevel difficulty) {
        try {
            // Проверяем rate limit
            if (shouldThrottleRequest()) {
                long waitTime = calculateWaitTime();
                log.info("Rate limit active, waiting {} seconds", waitTime);

                if (waitTime > 30) {
                    log.warn("Wait time too long ({}s), using fallback", waitTime);
                    return null;
                }

                Thread.sleep(waitTime * 1000);
            }

            String url = buildUrl(1, difficulty); // Всегда загружаем только 1 вопрос
            log.info("Loading single question from OpenTDB: {}", url);

            OpenTDBResponse response = restTemplate.getForObject(url, OpenTDBResponse.class);
            updateLastApiCall();

            if (response == null || response.getResponseCode() != 0) {
                log.error("OpenTDB returned error. Response code: {}",
                        response != null ? response.getResponseCode() : "null");
                handleApiError(response != null ? response.getResponseCode() : -1);
                return null;
            }

            if (response.getResults().isEmpty()) {
                log.warn("No questions returned for {} difficulty", difficulty);
                return null;
            }

            Question question = mapToQuestion(response.getResults().get(0), difficulty);
            log.info("Successfully loaded question for {} difficulty", difficulty);
            return question;

        } catch (Exception e) {
            log.error("Error loading single question for difficulty: " + difficulty, e);
            return null;
        }
    }

    private boolean shouldThrottleRequest() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(lastApiCall.plusSeconds(MIN_REQUEST_INTERVAL));
    }

    private long calculateWaitTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextAllowedTime = lastApiCall.plusSeconds(MIN_REQUEST_INTERVAL);
        return Math.max(0, java.time.Duration.between(now, nextAllowedTime).getSeconds());
    }

    private void updateLastApiCall() {
        lastApiCall = LocalDateTime.now();
    }

    private void handleApiError(int responseCode) {
        switch (responseCode) {
            case 5:
                log.warn("Rate limit exceeded - will wait longer before next request");
                // При rate limit увеличиваем время ожидания
                lastApiCall = LocalDateTime.now().plusSeconds(MIN_REQUEST_INTERVAL * 2);
                break;
            case 1:
                log.warn("No results returned by OpenTDB");
                break;
            case 2:
                log.warn("Invalid parameter in OpenTDB request");
                break;
            default:
                log.warn("OpenTDB error code: {}", responseCode);
        }
    }

    private String buildUrl(int amount, DifficultyLevel difficulty) {
        return String.format("%s?amount=%d&difficulty=%s&type=multiple&encode=url3986",
                BASE_URL, amount, difficulty.getApiDifficulty());
    }

    private Question mapToQuestion(OpenTDBQuestion apiQuestion, DifficultyLevel difficulty) {
        // Переводим тексты на русский
        String questionText = translationService.translateToRussian(decodeHtml(apiQuestion.getQuestion()));
        String correctAnswer = translationService.translateToRussian(decodeHtml(apiQuestion.getCorrectAnswer()));
        List<String> incorrectAnswers = apiQuestion.getIncorrectAnswers().stream()
                .map(this::decodeHtml)
                .map(translationService::translateToRussian)
                .collect(Collectors.toList());

        List<String> allAnswers = new ArrayList<>();
        allAnswers.add(correctAnswer);
        allAnswers.addAll(incorrectAnswers);
        Collections.shuffle(allAnswers);

        return Question.builder()
                .questionText(questionText)
                .correctAnswer(correctAnswer)
                .incorrectAnswers(incorrectAnswers)
                .allAnswers(allAnswers)
                .correctAnswerIndex(allAnswers.indexOf(correctAnswer))
                .category(translationService.translateToRussian(apiQuestion.getCategory()))
                .difficulty(difficulty)
                .build();
    }

    private String decodeHtml(String encoded) {
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                    .replace("&quot;", "\"")
                    .replace("&#039;", "'")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">");
        } catch (Exception e) {
            log.warn("Failed to decode HTML: {}", encoded);
            return encoded;
        }
    }

    /**
     * Получить статистику кэша
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
            Queue<Question> cache = questionCache.get(difficulty);
            stats.put(difficulty.name(), cache != null ? cache.size() : 0);
        }
        return stats;
    }
}