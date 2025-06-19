package com.example.cleopatra.config.diagnostic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Контроллер для нагрузочного тестирования
 * Позволяет тестировать производительность приложения
 */
@RestController
@RequestMapping("/diagnostic/load-test")
@RequiredArgsConstructor
@Slf4j
public class LoadTestController {

    private final OptimizedSystemMonitor systemMonitor;
    private final RestTemplate restTemplate = new RestTemplate();

    // Статистика тестирования
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private volatile boolean isTestRunning = false;

    /**
     * Запуск нагрузочного теста для профиля
     */
    @PostMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> startProfileLoadTest(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "100") int requests,
            @RequestParam(defaultValue = "10") int concurrency,
            @RequestParam(defaultValue = "false") boolean resetStats) {

        if (isTestRunning) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Тест уже запущен",
                    "status", "running"
            ));
        }

        if (resetStats) {
            resetStatistics();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result.put("targetUrl", "/profile/" + userId);
        result.put("totalRequests", requests);
        result.put("concurrency", concurrency);

        // Запуск асинхронного теста
        CompletableFuture.runAsync(() -> performLoadTest(userId, requests, concurrency));

        result.put("status", "started");
        result.put("message", "Нагрузочный тест запущен");

        return ResponseEntity.ok(result);
    }

    /**
     * Простой синхронный тест - для быстрой проверки
     */
    @PostMapping("/simple-test")
    public ResponseEntity<Map<String, Object>> runSimpleTest(
            @RequestParam(defaultValue = "http://localhost:8080/profile/4") String url,
            @RequestParam(defaultValue = "50") int requests) {

        log.info("🚀 Запуск простого теста: {} запросов к {}", requests, url);

        Map<String, Object> result = new HashMap<>();
        result.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        long startTime = System.currentTimeMillis();
        int successful = 0;
        int failed = 0;
        long totalTime = 0;

        // Получаем состояние системы ДО теста
        Map<String, Object> systemBefore = systemMonitor.getSystemInfo();

        for (int i = 0; i < requests; i++) {
            try {
                long requestStart = System.currentTimeMillis();

                // Выполняем HTTP запрос
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                long requestTime = System.currentTimeMillis() - requestStart;
                totalTime += requestTime;

                if (response.getStatusCode().is2xxSuccessful()) {
                    successful++;
                } else {
                    failed++;
                    log.warn("Неуспешный ответ: {}", response.getStatusCode());
                }

                // Логируем прогресс каждые 10 запросов
                if ((i + 1) % 10 == 0) {
                    log.info("Выполнено {}/{} запросов", i + 1, requests);
                }

            } catch (Exception e) {
                failed++;
                log.error("Ошибка запроса #{}: {}", i + 1, e.getMessage());
            }
        }

        long totalTestTime = System.currentTimeMillis() - startTime;

        // Получаем состояние системы ПОСЛЕ теста
        Map<String, Object> systemAfter = systemMonitor.getSystemInfo();

        // Формируем результат
        result.put("endTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result.put("totalTestTime", totalTestTime + " мс");
        result.put("successfulRequests", successful);
        result.put("failedRequests", failed);
        result.put("averageResponseTime", successful > 0 ? (totalTime / successful) + " мс" : "N/A");
        result.put("requestsPerSecond", String.format("%.2f", (double) requests / (totalTestTime / 1000.0)));

        // Сравнение системных показателей
        result.put("systemBefore", systemBefore);
        result.put("systemAfter", systemAfter);

        log.info("✅ Тест завершен: {}/{} успешных запросов за {} мс",
                successful, requests, totalTestTime);

        return ResponseEntity.ok(result);
    }

    /**
     * Получение текущего статуса теста
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTestStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("isRunning", isTestRunning);
        status.put("successfulRequests", successfulRequests.get());
        status.put("failedRequests", failedRequests.get());
        status.put("totalRequests", successfulRequests.get() + failedRequests.get());

        if (successfulRequests.get() > 0) {
            status.put("averageResponseTime",
                    totalResponseTime.get() / successfulRequests.get() + " мс");
        }

        // Добавляем текущее состояние системы
        status.put("currentSystemState", systemMonitor.getSystemInfo());
        status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return ResponseEntity.ok(status);
    }

    /**
     * Остановка текущего теста
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopTest() {
        isTestRunning = false;

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Тест остановлен");
        result.put("finalStats", getTestStatus().getBody());

        return ResponseEntity.ok(result);
    }

    /**
     * Сброс статистики
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTime.set(0);

        return ResponseEntity.ok(Map.of(
                "message", "Статистика сброшена",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
    }

    /**
     * Выполнение нагрузочного теста (асинхронно)
     */
    private void performLoadTest(Long userId, int requests, int concurrency) {
        isTestRunning = true;
        String url = "https://cleopatra-brcc.onrender.com/profile/" + userId;

        log.info("🚀 Начинаем нагрузочный тест: {} запросов с конкурентностью {}", requests, concurrency);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        try {
            // Создаем задачи
            for (int i = 0; i < requests; i++) {
                final int requestNumber = i + 1;

                executor.submit(() -> {
                    if (!isTestRunning) {
                        return; // Тест остановлен
                    }

                    try {
                        long startTime = System.currentTimeMillis();

                        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                        long responseTime = System.currentTimeMillis() - startTime;
                        totalResponseTime.addAndGet(responseTime);

                        if (response.getStatusCode().is2xxSuccessful()) {
                            successfulRequests.incrementAndGet();
                        } else {
                            failedRequests.incrementAndGet();
                            log.warn("Запрос #{} неуспешен: {}", requestNumber, response.getStatusCode());
                        }

                        // Логируем прогресс
                        if (requestNumber % 50 == 0) {
                            log.info("Выполнено {} запросов", requestNumber);
                        }

                    } catch (Exception e) {
                        failedRequests.incrementAndGet();
                        log.error("Ошибка в запросе #{}: {}", requestNumber, e.getMessage());
                    }
                });
            }

        } finally {
            executor.shutdown();
            isTestRunning = false;
            log.info("✅ Нагрузочный тест завершен");
        }
    }

    /**
     * Получение детальной информации о системе во время теста
     */
    @GetMapping("/system-snapshot")
    public ResponseEntity<Map<String, Object>> getSystemSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();

        // Системная информация
        snapshot.put("system", systemMonitor.getSystemInfo());

        // Статистика теста
        snapshot.put("testStats", Map.of(
                "isRunning", isTestRunning,
                "successful", successfulRequests.get(),
                "failed", failedRequests.get(),
                "total", successfulRequests.get() + failedRequests.get()
        ));

        // Время снимка
        snapshot.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return ResponseEntity.ok(snapshot);
    }
}