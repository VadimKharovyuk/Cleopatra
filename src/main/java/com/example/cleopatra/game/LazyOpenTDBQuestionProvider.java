////package com.example.cleopatra.game;
////
////import com.example.cleopatra.enums.DifficultyLevel;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.stereotype.Service;
////import org.springframework.web.client.RestTemplate;
////import org.springframework.scheduling.annotation.Scheduled;
////
////import java.net.URLDecoder;
////import java.nio.charset.StandardCharsets;
////import java.time.LocalDateTime;
////import java.util.*;
////import java.util.concurrent.ConcurrentHashMap;
////import java.util.concurrent.ThreadLocalRandom;
////import java.util.stream.Collectors;
////
////@Service
////@Slf4j
////public class OpenTDBQuestionProvider implements QuestionProvider {
////
////    private final RestTemplate restTemplate;
////    private static final String BASE_URL = "https://opentdb.com/api.php";
////
////    // Кэш вопросов по уровню сложности
////    private final Map<DifficultyLevel, List<Question>> questionCache = new ConcurrentHashMap<>();
////
////    // Минимальное количество вопросов в кэше для каждого уровня
////    private static final int MIN_CACHE_SIZE = 50;
////
////    // Максимальное количество вопросов для загрузки за раз
////    private static final int BATCH_SIZE = 20;
////
////    // Время последнего запроса к API для rate limiting
////    private volatile LocalDateTime lastApiCall = LocalDateTime.now().minusSeconds(10);
////
////    // Минимальный интервал между запросами (в секундах)
////    private static final int MIN_REQUEST_INTERVAL = 8;
////
////    // Флаг для предотвращения одновременного выполнения пополнения кэша
////    private volatile boolean cacheReplenishmentInProgress = false;
////
////    // Флаг для предотвращения множественной инициализации
////    private static volatile boolean globalInitialLoadStarted = false;
////    private static final Object initLock = new Object();
////
////    public OpenTDBQuestionProvider(RestTemplate restTemplate) {
////        this.restTemplate = restTemplate;
////        String instanceId = Integer.toHexString(System.identityHashCode(this));
////        log.info("Creating OpenTDBQuestionProvider instance #{} - Thread: {}", instanceId, Thread.currentThread().getName());
////        // Инициализируем кэш при старте
////        initializeCache();
////    }
////
////    @Override
////    public Question getQuestion(DifficultyLevel difficulty) {
////        // Сначала пытаемся получить из кэша
////        Question cachedQuestion = getQuestionFromCache(difficulty);
////        if (cachedQuestion != null) {
////            return cachedQuestion;
////        }
////
////        // Если в кэше нет вопросов, пытаемся загрузить синхронно
////        List<Question> newQuestions = loadQuestionsWithRateLimit(difficulty, 5);
////        if (!newQuestions.isEmpty()) {
////            addQuestionsToCache(difficulty, newQuestions);
////            return newQuestions.get(0);
////        }
////
////        // Если не удалось загрузить, возвращаем fallback вопрос
////        return createFallbackQuestion(difficulty);
////    }
////
////    @Override
////    public List<Question> getQuestions(DifficultyLevel difficulty, int amount) {
////        List<Question> result = new ArrayList<>();
////
////        // Получаем из кэша сколько можем
////        List<Question> cachedQuestions = getQuestionsFromCache(difficulty, amount);
////        result.addAll(cachedQuestions);
////
////        // Если нужно больше, пытаемся загрузить
////        int remaining = amount - result.size();
////        if (remaining > 0) {
////            List<Question> newQuestions = loadQuestionsWithRateLimit(difficulty, remaining);
////            result.addAll(newQuestions);
////
////            // Добавляем в кэш лишние вопросы
////            if (newQuestions.size() > remaining) {
////                List<Question> forCache = newQuestions.subList(remaining, newQuestions.size());
////                addQuestionsToCache(difficulty, forCache);
////            }
////        }
////
////        return result;
////    }
////
////    @Override
////    public boolean isAvailable() {
////        try {
////            // Проверяем доступность только если прошло достаточно времени
////            if (shouldThrottleRequest()) {
////                return true; // Предполагаем, что доступен
////            }
////
////            String testUrl = BASE_URL + "?amount=1&type=multiple";
////            OpenTDBResponse response = restTemplate.getForObject(testUrl, OpenTDBResponse.class);
////            updateLastApiCall();
////            return response != null && response.getResponseCode() == 0;
////        } catch (Exception e) {
////            log.warn("OpenTDB availability check failed", e);
////            return false;
////        }
////    }
////
////    /**
////     * Периодически пополняем кэш в фоновом режиме
////     */
////    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // каждые 5 минут, начальная задержка 1 минута
////    public void replenishCache() {
////        // Предотвращаем одновременное выполнение
////        if (cacheReplenishmentInProgress) {
////            log.debug("Cache replenishment already in progress, skipping");
////            return;
////        }
////
////        cacheReplenishmentInProgress = true;
////        try {
////            log.info("Starting background cache replenishment");
////
////            for (DifficultyLevel difficulty : DifficultyLevel.values()) {
////                try {
////                    int currentCacheSize = questionCache.getOrDefault(difficulty, Collections.emptyList()).size();
////
////                    if (currentCacheSize < MIN_CACHE_SIZE) {
////                        int needed = MIN_CACHE_SIZE - currentCacheSize;
////                        log.info("Replenishing cache for {} difficulty. Current: {}, needed: {}",
////                                difficulty, currentCacheSize, needed);
////
////                        List<Question> newQuestions = loadQuestionsWithRateLimit(difficulty, needed);
////                        if (!newQuestions.isEmpty()) {
////                            addQuestionsToCache(difficulty, newQuestions);
////                            log.info("Added {} questions to {} cache", newQuestions.size(), difficulty);
////                        } else {
////                            log.warn("No questions loaded for {} difficulty", difficulty);
////                        }
////
////                        // Увеличенная пауза между загрузками разных уровней сложности
////                        Thread.sleep(8000);
////                    }
////                } catch (Exception e) {
////                    log.error("Error replenishing cache for difficulty: " + difficulty, e);
////                    // Продолжаем с следующим уровнем сложности даже если текущий упал
////                }
////            }
////            log.info("Background cache replenishment completed");
////        } finally {
////            cacheReplenishmentInProgress = false;
////        }
////    }
////
////    // ============ Приватные методы ============
////
////    private void initializeCache() {
////        log.info("Initializing question cache");
////        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
////            questionCache.put(difficulty, Collections.synchronizedList(new ArrayList<>()));
////        }
////
////        // Загружаем начальный набор вопросов в отдельном потоке
////        // Используем глобальную синхронизацию для предотвращения множественного запуска
////        synchronized (initLock) {
////            if (!globalInitialLoadStarted) {
////                globalInitialLoadStarted = true;
////                log.info("Starting initial questions loading thread");
////                new Thread(this::loadInitialQuestions, "initial-questions-loader").start();
////            } else {
////                log.info("Initial loading already started, skipping");
////            }
////        }
////    }
////
////    private void loadInitialQuestions() {
////        try {
////            log.info("Starting initial questions loading (Thread: {})", Thread.currentThread().getName());
////
////            // Задержка перед началом загрузки
////            Thread.sleep(5000);
////
////            for (DifficultyLevel difficulty : DifficultyLevel.values()) {
////                try {
////                    log.info("Loading initial questions for {} difficulty", difficulty);
////                    List<Question> questions = loadQuestionsWithRateLimit(difficulty, 3); // Еще больше уменьшаем
////                    if (!questions.isEmpty()) {
////                        addQuestionsToCache(difficulty, questions);
////                        log.info("Loaded {} initial questions for {} difficulty", questions.size(), difficulty);
////                    } else {
////                        log.warn("Failed to load initial questions for {} difficulty", difficulty);
////                    }
////                    Thread.sleep(12000); // Увеличиваем паузу до 12 секунд
////                } catch (InterruptedException e) {
////                    Thread.currentThread().interrupt();
////                    log.warn("Initial loading interrupted");
////                    break;
////                } catch (Exception e) {
////                    log.error("Error loading initial questions for " + difficulty, e);
////                    // Продолжаем со следующим уровнем
////                }
////            }
////            log.info("Initial cache loading completed");
////        } catch (Exception e) {
////            log.error("Error during initial cache loading", e);
////        }
////    }
////
////    private Question getQuestionFromCache(DifficultyLevel difficulty) {
////        List<Question> cache = questionCache.get(difficulty);
////        if (cache != null && !cache.isEmpty()) {
////            synchronized (cache) {
////                if (!cache.isEmpty()) {
////                    return cache.remove(ThreadLocalRandom.current().nextInt(cache.size()));
////                }
////            }
////        }
////        return null;
////    }
////
////    private List<Question> getQuestionsFromCache(DifficultyLevel difficulty, int amount) {
////        List<Question> result = new ArrayList<>();
////        List<Question> cache = questionCache.get(difficulty);
////
////        if (cache != null && !cache.isEmpty()) {
////            synchronized (cache) {
////                int available = Math.min(amount, cache.size());
////                for (int i = 0; i < available; i++) {
////                    if (!cache.isEmpty()) {
////                        result.add(cache.remove(ThreadLocalRandom.current().nextInt(cache.size())));
////                    }
////                }
////            }
////        }
////
////        return result;
////    }
////
////    private void addQuestionsToCache(DifficultyLevel difficulty, List<Question> questions) {
////        List<Question> cache = questionCache.get(difficulty);
////        if (cache != null) {
////            synchronized (cache) {
////                cache.addAll(questions);
////                log.debug("Added {} questions to {} cache. Total: {}",
////                        questions.size(), difficulty, cache.size());
////            }
////        }
////    }
////
////    private List<Question> loadQuestionsWithRateLimit(DifficultyLevel difficulty, int amount) {
////        try {
////            // Проверяем rate limit
////            if (shouldThrottleRequest()) {
////                long waitTime = calculateWaitTime();
////                log.warn("Rate limit active, need to wait {} seconds", waitTime);
////
////                // Если ожидание слишком долгое, не делаем запрос
////                if (waitTime > 30) {
////                    log.warn("Wait time too long ({}s), skipping request", waitTime);
////                    return Collections.emptyList();
////                }
////
////                try {
////                    Thread.sleep(waitTime * 1000);
////                } catch (InterruptedException e) {
////                    Thread.currentThread().interrupt();
////                    return Collections.emptyList();
////                }
////            }
////
////            // Ограничиваем количество запрашиваемых вопросов
////            int requestAmount = Math.min(amount, BATCH_SIZE);
////
////            String url = buildUrl(requestAmount, difficulty);
////            log.info("Fetching {} questions from OpenTDB: {}", requestAmount, url);
////
////            OpenTDBResponse response = restTemplate.getForObject(url, OpenTDBResponse.class);
////            updateLastApiCall();
////
////            if (response == null) {
////                log.error("Received null response from OpenTDB");
////                return Collections.emptyList();
////            }
////
////            if (response.getResponseCode() != 0) {
////                log.error("OpenTDB returned error code: {}", response.getResponseCode());
////                handleApiError(response.getResponseCode());
////                return Collections.emptyList();
////            }
////
////            List<Question> questions = response.getResults().stream()
////                    .map(apiQuestion -> mapToQuestion(apiQuestion, difficulty))
////                    .collect(Collectors.toList());
////
////            log.info("Successfully loaded {} questions for {} difficulty", questions.size(), difficulty);
////            return questions;
////
////        } catch (Exception e) {
////            log.error("Error fetching questions from OpenTDB for difficulty: " + difficulty, e);
////            return Collections.emptyList();
////        }
////    }
////
////    private boolean shouldThrottleRequest() {
////        LocalDateTime now = LocalDateTime.now();
////        return now.isBefore(lastApiCall.plusSeconds(MIN_REQUEST_INTERVAL));
////    }
////
////    private long calculateWaitTime() {
////        LocalDateTime now = LocalDateTime.now();
////        LocalDateTime nextAllowedTime = lastApiCall.plusSeconds(MIN_REQUEST_INTERVAL);
////        return java.time.Duration.between(now, nextAllowedTime).getSeconds();
////    }
////
////    private void updateLastApiCall() {
////        lastApiCall = LocalDateTime.now();
////    }
////
////    private void handleApiError(int responseCode) {
////        switch (responseCode) {
////            case 1:
////                log.warn("No results returned by OpenTDB");
////                break;
////            case 2:
////                log.warn("Invalid parameter in OpenTDB request");
////                break;
////            case 3:
////                log.warn("Token not found in OpenTDB");
////                break;
////            case 4:
////                log.warn("Token empty in OpenTDB - all questions exhausted");
////                break;
////            case 5:
////                log.warn("Rate limit exceeded for OpenTDB - will wait longer before next request");
////                // При rate limit увеличиваем время ожидания
////                lastApiCall = LocalDateTime.now().plusSeconds(MIN_REQUEST_INTERVAL * 2);
////                break;
////            default:
////                log.warn("Unknown error code from OpenTDB: {}", responseCode);
////        }
////    }
////
////    private Question createFallbackQuestion(DifficultyLevel difficulty) {
////        // Создаем простой fallback вопрос если API недоступен
////        List<String> answers = Arrays.asList("A", "B", "C", "D");
////
////        return Question.builder()
////                .questionText("Это резервный вопрос. Выберите правильный ответ: A")
////                .correctAnswer("A")
////                .incorrectAnswers(Arrays.asList("B", "C", "D"))
////                .allAnswers(answers)
////                .correctAnswerIndex(0)
////                .category("General Knowledge")
////                .difficulty(difficulty)
////                .build();
////    }
////
////    private String buildUrl(int amount, DifficultyLevel difficulty) {
////        return String.format("%s?amount=%d&difficulty=%s&type=multiple&encode=url3986",
////                BASE_URL, amount, difficulty.getApiDifficulty());
////    }
////
////    private Question mapToQuestion(OpenTDBQuestion apiQuestion, DifficultyLevel difficulty) {
////        // Декодируем HTML entities
////        String questionText = decodeHtml(apiQuestion.getQuestion());
////        String correctAnswer = decodeHtml(apiQuestion.getCorrectAnswer());
////        List<String> incorrectAnswers = apiQuestion.getIncorrectAnswers().stream()
////                .map(this::decodeHtml)
////                .collect(Collectors.toList());
////
////        // Создаем список всех ответов и перемешиваем
////        List<String> allAnswers = new ArrayList<>();
////        allAnswers.add(correctAnswer);
////        allAnswers.addAll(incorrectAnswers);
////        Collections.shuffle(allAnswers);
////
////        return Question.builder()
////                .questionText(questionText)
////                .correctAnswer(correctAnswer)
////                .incorrectAnswers(incorrectAnswers)
////                .allAnswers(allAnswers)
////                .correctAnswerIndex(allAnswers.indexOf(correctAnswer))
////                .category(apiQuestion.getCategory())
////                .difficulty(difficulty)
////                .build();
////    }
////
////    private String decodeHtml(String encoded) {
////        try {
////            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
////                    .replace("&quot;", "\"")
////                    .replace("&#039;", "'")
////                    .replace("&amp;", "&")
////                    .replace("&lt;", "<")
////                    .replace("&gt;", ">");
////        } catch (Exception e) {
////            log.warn("Failed to decode HTML: {}", encoded);
////            return encoded;
////        }
////    }
////
////    /**
////     * Получить статистику кэша для мониторинга
////     */
////    public Map<String, Integer> getCacheStats() {
////        Map<String, Integer> stats = new HashMap<>();
////        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
////            List<Question> cache = questionCache.get(difficulty);
////            stats.put(difficulty.name(), cache != null ? cache.size() : 0);
////        }
////        return stats;
////    }
////}
//
//package com.example.cleopatra.game;
//
//import com.example.cleopatra.enums.DifficultyLevel;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.net.URLDecoder;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class LazyOpenTDBQuestionProvider implements QuestionProvider {
//
//    private final RestTemplate restTemplate;
//    private final FallbackQuestionProvider fallbackProvider;
//    private static final String BASE_URL = "https://opentdb.com/api.php";
//
//    // Небольшой кэш для текущего уровня - максимум 5 вопросов
//    private final Map<DifficultyLevel, Queue<Question>> questionCache = new ConcurrentHashMap<>();
//
//    // Время последнего запроса к API для rate limiting
//    private volatile LocalDateTime lastApiCall = LocalDateTime.now().minusSeconds(15);
//
//    // Минимальный интервал между запросами (в секундах)
//    private static final int MIN_REQUEST_INTERVAL = 10;
//
//    public LazyOpenTDBQuestionProvider(RestTemplate restTemplate, FallbackQuestionProvider fallbackProvider) {
//        this.restTemplate = restTemplate;
//        this.fallbackProvider = fallbackProvider;
//        log.info("Created LazyOpenTDBQuestionProvider - questions will be loaded on demand");
//
//        // Инициализируем пустые очереди
//        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
//            questionCache.put(difficulty, new LinkedList<>());
//        }
//    }
//
//    @Override
//    public Question getQuestion(DifficultyLevel difficulty) {
//        // Сначала пытаемся получить из кэша
//        Question cachedQuestion = getQuestionFromCache(difficulty);
//        if (cachedQuestion != null) {
//            log.debug("Returning cached question for {} difficulty", difficulty);
//            return cachedQuestion;
//        }
//
//        // Если в кэше нет, пытаемся загрузить ОДИН вопрос
//        Question freshQuestion = loadSingleQuestion(difficulty);
//        if (freshQuestion != null) {
//            log.info("Loaded fresh question for {} difficulty", difficulty);
//            return freshQuestion;
//        }
//
//        // Если не удалось загрузить, используем fallback
//        log.warn("Using fallback question for {} difficulty", difficulty);
//        return fallbackProvider.getQuestion(difficulty);
//    }
//
//    @Override
//    public List<Question> getQuestions(DifficultyLevel difficulty, int amount) {
//        List<Question> result = new ArrayList<>();
//
//        // Получаем по одному вопросу
//        for (int i = 0; i < amount; i++) {
//            Question question = getQuestion(difficulty);
//            if (question != null) {
//                result.add(question);
//
//                // Небольшая пауза между получениями вопросов если нужно больше одного
//                if (i < amount - 1) {
//                    try {
//                        Thread.sleep(1000); // 1 секунда между вопросами
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        break;
//                    }
//                }
//            }
//        }
//
//        return result;
//    }
//
//
//
//    @Override
//    public boolean isAvailable() {
//        try {
//            // Проверяем только если прошло достаточно времени
//            if (shouldThrottleRequest()) {
//                return true; // Предполагаем что доступен
//            }
//
//            String testUrl = BASE_URL + "?amount=1&type=multiple&difficulty=easy";
//            OpenTDBResponse response = restTemplate.getForObject(testUrl, OpenTDBResponse.class);
//            updateLastApiCall();
//            return response != null && response.getResponseCode() == 0;
//        } catch (Exception e) {
//            log.debug("OpenTDB availability check failed: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * Предварительно загружаем несколько вопросов для указанного уровня
//     * Используется когда игрок приближается к новому уровню сложности
//     */
//    public void preloadQuestionsForLevel(DifficultyLevel difficulty) {
//        log.info("Preloading questions for {} difficulty", difficulty);
//
//        // Загружаем 2-3 вопроса заранее
//        new Thread(() -> {
//            try {
//                for (int i = 0; i < 3; i++) {
//                    if (shouldThrottleRequest()) {
//                        long waitTime = calculateWaitTime();
//                        log.info("Waiting {} seconds before preloading question {}", waitTime, i + 1);
//                        Thread.sleep(waitTime * 1000);
//                    }
//
//                    Question question = loadSingleQuestion(difficulty);
//                    if (question != null) {
//                        addQuestionToCache(difficulty, question);
//                        log.info("Preloaded question {} for {} difficulty", i + 1, difficulty);
//                    } else {
//                        log.warn("Failed to preload question {} for {} difficulty", i + 1, difficulty);
//                        break; // Прекращаем если не удалось загрузить
//                    }
//
//                    // Пауза между загрузками
//                    Thread.sleep(MIN_REQUEST_INTERVAL * 1000);
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                log.info("Preloading interrupted for {} difficulty", difficulty);
//            } catch (Exception e) {
//                log.error("Error during preloading for " + difficulty, e);
//            }
//        }, "preload-" + difficulty.name().toLowerCase()).start();
//    }
//
//    // ============ Приватные методы ============
//
//    private Question getQuestionFromCache(DifficultyLevel difficulty) {
//        Queue<Question> cache = questionCache.get(difficulty);
//        if (cache != null && !cache.isEmpty()) {
//            synchronized (cache) {
//                return cache.poll();
//            }
//        }
//        return null;
//    }
//
//    private void addQuestionToCache(DifficultyLevel difficulty, Question question) {
//        Queue<Question> cache = questionCache.get(difficulty);
//        if (cache != null) {
//            synchronized (cache) {
//                // Ограничиваем размер кэша
//                if (cache.size() >= 5) {
//                    cache.poll(); // Удаляем старый вопрос
//                }
//                cache.offer(question);
//                log.debug("Added question to {} cache. Cache size: {}", difficulty, cache.size());
//            }
//        }
//    }
//
//    private Question loadSingleQuestion(DifficultyLevel difficulty) {
//        try {
//            // Проверяем rate limit
//            if (shouldThrottleRequest()) {
//                long waitTime = calculateWaitTime();
//                log.info("Rate limit active, waiting {} seconds", waitTime);
//
//                if (waitTime > 30) {
//                    log.warn("Wait time too long ({}s), using fallback", waitTime);
//                    return null;
//                }
//
//                Thread.sleep(waitTime * 1000);
//            }
//
//            String url = buildUrl(1, difficulty); // Всегда загружаем только 1 вопрос
//            log.info("Loading single question from OpenTDB: {}", url);
//
//            OpenTDBResponse response = restTemplate.getForObject(url, OpenTDBResponse.class);
//            updateLastApiCall();
//
//            if (response == null || response.getResponseCode() != 0) {
//                log.error("OpenTDB returned error. Response code: {}",
//                        response != null ? response.getResponseCode() : "null");
//                handleApiError(response != null ? response.getResponseCode() : -1);
//                return null;
//            }
//
//            if (response.getResults().isEmpty()) {
//                log.warn("No questions returned for {} difficulty", difficulty);
//                return null;
//            }
//
//            Question question = mapToQuestion(response.getResults().get(0), difficulty);
//            log.info("Successfully loaded question for {} difficulty", difficulty);
//            return question;
//
//        } catch (Exception e) {
//            log.error("Error loading single question for difficulty: " + difficulty, e);
//            return null;
//        }
//    }
//
//    private boolean shouldThrottleRequest() {
//        LocalDateTime now = LocalDateTime.now();
//        return now.isBefore(lastApiCall.plusSeconds(MIN_REQUEST_INTERVAL));
//    }
//
//    private long calculateWaitTime() {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime nextAllowedTime = lastApiCall.plusSeconds(MIN_REQUEST_INTERVAL);
//        return Math.max(0, java.time.Duration.between(now, nextAllowedTime).getSeconds());
//    }
//
//    private void updateLastApiCall() {
//        lastApiCall = LocalDateTime.now();
//    }
//
//    private void handleApiError(int responseCode) {
//        switch (responseCode) {
//            case 5:
//                log.warn("Rate limit exceeded - will wait longer before next request");
//                // При rate limit увеличиваем время ожидания
//                lastApiCall = LocalDateTime.now().plusSeconds(MIN_REQUEST_INTERVAL * 2);
//                break;
//            case 1:
//                log.warn("No results returned by OpenTDB");
//                break;
//            case 2:
//                log.warn("Invalid parameter in OpenTDB request");
//                break;
//            default:
//                log.warn("OpenTDB error code: {}", responseCode);
//        }
//    }
//
//    private String buildUrl(int amount, DifficultyLevel difficulty) {
//        return String.format("%s?amount=%d&difficulty=%s&type=multiple&encode=url3986",
//                BASE_URL, amount, difficulty.getApiDifficulty());
//    }
//
//    private Question mapToQuestion(OpenTDBQuestion apiQuestion, DifficultyLevel difficulty) {
//        String questionText = decodeHtml(apiQuestion.getQuestion());
//        String correctAnswer = decodeHtml(apiQuestion.getCorrectAnswer());
//        List<String> incorrectAnswers = apiQuestion.getIncorrectAnswers().stream()
//                .map(this::decodeHtml)
//                .collect(Collectors.toList());
//
//        List<String> allAnswers = new ArrayList<>();
//        allAnswers.add(correctAnswer);
//        allAnswers.addAll(incorrectAnswers);
//        Collections.shuffle(allAnswers);
//
//        return Question.builder()
//                .questionText(questionText)
//                .correctAnswer(correctAnswer)
//                .incorrectAnswers(incorrectAnswers)
//                .allAnswers(allAnswers)
//                .correctAnswerIndex(allAnswers.indexOf(correctAnswer))
//                .category(apiQuestion.getCategory())
//                .difficulty(difficulty)
//                .build();
//    }
//
//    private String decodeHtml(String encoded) {
//        try {
//            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
//                    .replace("&quot;", "\"")
//                    .replace("&#039;", "'")
//                    .replace("&amp;", "&")
//                    .replace("&lt;", "<")
//                    .replace("&gt;", ">");
//        } catch (Exception e) {
//            log.warn("Failed to decode HTML: {}", encoded);
//            return encoded;
//        }
//    }
//
//    /**
//     * Получить статистику кэша
//     */
//    public Map<String, Integer> getCacheStats() {
//        Map<String, Integer> stats = new HashMap<>();
//        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
//            Queue<Question> cache = questionCache.get(difficulty);
//            stats.put(difficulty.name(), cache != null ? cache.size() : 0);
//        }
//        return stats;
//    }
//}

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