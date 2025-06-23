package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.maper.UserMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final UserService userService;
    private final UserMapper userMapper;



    // ✅ БЕЗОПАСНЫЙ тест - проверяем что загружается
    @GetMapping("/check-user-loading")
    public ResponseEntity<Map<String, Object>> checkUserLoading(Authentication authentication) {

        Map<String, Object> debugInfo = new HashMap<>();

        try {
            String username = authentication.getName();
            log.info("=== НАЧАЛО ТЕСТА ЗАГРУЗКИ ПОЛЬЗОВАТЕЛЯ ===");
            log.info("Username: {}", username);

            // Засекаем время
            long startTime = System.currentTimeMillis();

            // ЗДЕСЬ ВАШ ОПАСНЫЙ ВЫЗОВ
            UserResponse currentUser = userService.getUserByEmail(username);

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.info("=== РЕЗУЛЬТАТЫ ТЕСТА ===");
            log.info("Время выполнения: {} мс", executionTime);
            log.info("User ID: {}", currentUser != null ? currentUser.getId() : "null");

            // Собираем debug информацию
            debugInfo.put("success", true);
            debugInfo.put("executionTimeMs", executionTime);
            debugInfo.put("username", username);
            debugInfo.put("userId", currentUser != null ? currentUser.getId() : null);
            debugInfo.put("userFound", currentUser != null);

            // ВАЖНО: НЕ возвращаем сам объект User!
            // Возвращаем только метаинформацию

            log.info("=== КОНЕЦ ТЕСТА ===");

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            log.error("ОШИБКА в тесте загрузки пользователя", e);

            debugInfo.put("success", false);
            debugInfo.put("error", e.getMessage());
            debugInfo.put("errorType", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(debugInfo);
        }
    }

    // ✅ БЕЗОПАСНЫЙ тест - проверяем SQL запросы
    @GetMapping("/check-sql-queries")
    public ResponseEntity<Map<String, Object>> checkSqlQueries(Authentication authentication) {

        Map<String, Object> result = new HashMap<>();

        try {
            String username = authentication.getName();

            // Включаем логирование SQL (если настроено)
            log.info("=== МОНИТОРИНГ SQL ЗАПРОСОВ ===");
            log.info("Проверяем сколько SQL запросов выполнится для getUserByEmail()");

            long startTime = System.currentTimeMillis();

            // Ваш метод, который нужно проверить
            UserResponse user = userService.getUserByEmail(username);

            long endTime = System.currentTimeMillis();

            result.put("executionTimeMs", endTime - startTime);
            result.put("userFound", user != null);
            result.put("message", "Проверьте логи - там должны быть SQL запросы");
            result.put("warning", "Если видите много SELECT запросов - это проблема N+1!");

            log.info("=== ЗАВЕРШЕН МОНИТОРИНГ SQL ===");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Ошибка при мониторинге SQL", e);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    // ✅ СРАВНЕНИЕ производительности разных подходов
    @GetMapping("/compare-approaches")
    public ResponseEntity<Map<String, Object>> compareApproaches(Authentication authentication) {

        Map<String, Object> comparison = new HashMap<>();
        String username = authentication.getName();

        try {
            // Тест 1: Ваш текущий метод
            long start1 = System.currentTimeMillis();
            UserResponse user1 = userService.getUserByEmail(username);
            long time1 = System.currentTimeMillis() - start1;

            // Тест 2: Безопасный метод с DTO (если есть)
            long start2 = System.currentTimeMillis();
            // UserDto user2 = userService.getUserDtoByEmail(username); // Создайте этот метод
            long time2 = System.currentTimeMillis() - start2;

            // Тест 3: Только ID пользователя
            long start3 = System.currentTimeMillis();
            Long userId = userService.getUserIdByEmail(username);
            long time3 = System.currentTimeMillis() - start3;

            comparison.put("currentMethod", Map.of(
                    "timeMs", time1,
                    "description", "userService.getUserByEmail() - текущий метод"
            ));

            comparison.put("dtoMethod", Map.of(
                    "timeMs", time2,
                    "description", "getUserDtoByEmail() - рекомендуемый метод"
            ));

            comparison.put("idOnlyMethod", Map.of(
                    "timeMs", time3,
                    "description", "getUserIdByEmail() - самый быстрый метод"
            ));

            comparison.put("recommendation",
                    time1 > 100 ? "КРИТИЧНО: Текущий метод слишком медленный!" :
                            time1 > 50 ? "ВНИМАНИЕ: Есть место для оптимизации" :
                                    "OK: Производительность приемлема");

            return ResponseEntity.ok(comparison);

        } catch (Exception e) {
            log.error("Ошибка при сравнении подходов", e);
            comparison.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(comparison);
        }
    }

    // ✅ ДЕТАЛЬНАЯ проверка какие поля загружаются
    @GetMapping("/analyze-user-fields")
    public ResponseEntity<Map<String, Object>> analyzeUserFields(Authentication authentication) {

        Map<String, Object> analysis = new HashMap<>();

        try {
            String username = authentication.getName();
            log.info("=== АНАЛИЗ ПОЛЕЙ ПОЛЬЗОВАТЕЛЯ ===");

            UserResponse user = userService.getUserByEmail(username);

            if (user != null) {
                analysis.put("id", user.getId() != null);
                analysis.put("email", user.getEmail() != null);
                analysis.put("firstName", user.getFirstName() != null);
                analysis.put("lastName", user.getLastName() != null);
                analysis.put("imageUrl", user.getImageUrl() != null);
                // Добавьте проверки для всех полей UserResponse

                analysis.put("totalFieldsLoaded", analysis.size());
                analysis.put("recommendation", "Убедитесь что UserResponse содержит только нужные поля");
            } else {
                analysis.put("error", "Пользователь не найден");
            }

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("Ошибка при анализе полей", e);
            analysis.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(analysis);
        }
    }

    @GetMapping("/stress-test")
    public ResponseEntity<Map<String, Object>> stressTest(Authentication authentication) {

        Map<String, Object> results = new HashMap<>();
        String username = authentication.getName();

        try {
            // Тест множественных вызовов
            long totalTime = 0;
            int iterations = 100;

            for (int i = 0; i < iterations; i++) {
                long start = System.currentTimeMillis();

                UserResponse user = userService.getUserByEmail(username);

                long end = System.currentTimeMillis();
                totalTime += (end - start);
            }

            long avgTime = totalTime / iterations;

            results.put("iterations", iterations);
            results.put("totalTimeMs", totalTime);
            results.put("averageTimeMs", avgTime);
            results.put("maxAcceptableMs", 50);
            results.put("status", avgTime < 50 ? "EXCELLENT" :
                    avgTime < 100 ? "GOOD" :
                            avgTime < 200 ? "ACCEPTABLE" : "NEEDS_OPTIMIZATION");

            results.put("recommendation",
                    avgTime < 50 ? "Производительность отличная!" :
                            avgTime < 100 ? "Хорошая производительность" :
                                    "Рассмотрите кэширование или оптимизацию запросов");

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("Ошибка стресс-теста", e);
            results.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(results);
        }
    }


    // ✅ Основной профиль - безопасно
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication auth) {
        UserResponse user = userService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(user);
    }

}
