package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.service.PostService;
import com.example.cleopatra.service.UserService;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cache")
public class CacheController {
    private final CacheManager cacheManager;
    private final PostService postService;
    private final UserService userService;

    @GetMapping("/stress-test/{duration}")
    public ResponseEntity<Map<String, Object>> stressTest(@PathVariable int duration) {

        // ✅ РЕАЛЬНЫЕ ID постов из вашей БД
        Long[] availablePostIds = {2L, 12L, 13L, 14L, 15L, 16L, 19L, 72L, 73L, 75L,
                76L, 78L, 82L, 85L, 118L, 119L, 120L, 122L, 123L};

        // ✅ РЕАЛЬНЫЕ ID пользователей
        Long[] availableUserIds = {1L, 5L, 6L, 7L};

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (duration * 1000L);
        int totalRequests = 0;
        int userRequests = 0;
        int postRequests = 0;
        int userPostsRequests = 0;
        int analyticsRequests = 0;

        Random random = new Random();

        try {
            while (System.currentTimeMillis() < endTime) {
                // Выбираем случайные СУЩЕСТВУЮЩИЕ ID
                Long userId = availableUserIds[random.nextInt(availableUserIds.length)];
                Long postId = availablePostIds[random.nextInt(availablePostIds.length)];

                int choice = random.nextInt(100);

                try {
                    // 40% - профили пользователей
                    if (choice < 40) {
                        userService.getUserById(userId);
                        userRequests++;
                    }
                    // 30% - посты
                    else if (choice < 70) {
                        postService.getPostById(postId);
                        postRequests++;
                    }
                    // 20% - списки постов
                    else if (choice < 90) {
                        postService.getUserPosts(userId, 0, 10);
                        userPostsRequests++;
                    }
                    // 10% - аналитика
                    else {
                        userService.getTotalUsersCount();
                        analyticsRequests++;
                    }

                    totalRequests++;

                } catch (Exception e) {
                    System.err.println("Error in request: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Test failed: " + e.getMessage());
            errorResult.put("totalRequestsCompleted", totalRequests);
            return ResponseEntity.status(500).body(errorResult);
        }

        long actualDuration = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("duration", duration);
        result.put("actualDurationMs", actualDuration);
        result.put("totalRequests", totalRequests);
        result.put("requestsPerSecond", totalRequests > 0 ? (totalRequests * 1000.0) / actualDuration : 0);
        result.put("userRequests", userRequests);
        result.put("postRequests", postRequests);
        result.put("userPostsRequests", userPostsRequests);
        result.put("analyticsRequests", analyticsRequests);
        result.put("method", "stress-test-mixed");
        result.put("availablePostIds", availablePostIds.length);
        result.put("availableUserIds", availableUserIds.length);

        return ResponseEntity.ok(result);
    }

    // ===================  ПОЛЬЗОВАТЕЛИ  ===================

    // GET /api/cache/load-test-user/1/10000
    @GetMapping("/load-test-user/{userId}/{requests}")
    public ResponseEntity<Map<String, Object>> loadTestUser(
            @PathVariable Long userId,
            @PathVariable int requests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            userService.getUserById(userId);
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("requests", requests);
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) requests);
        result.put("method", "getUserById");

        return ResponseEntity.ok(result);
    }

    // GET /api/cache/load-test-user-email/test@example.com/5000
    @GetMapping("/load-test-user-email/{email}/{requests}")
    public ResponseEntity<Map<String, Object>> loadTestUserByEmail(
            @PathVariable String email,
            @PathVariable int requests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            userService.getUserByEmail(email);
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("email", email);
        result.put("requests", requests);
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) requests);
        result.put("method", "getUserByEmail");

        return ResponseEntity.ok(result);
    }

    // GET /api/cache/load-test-user-status/1/5000
    @GetMapping("/load-test-user-status/{userId}/{requests}")
    public ResponseEntity<Map<String, Object>> loadTestUserStatus(
            @PathVariable Long userId,
            @PathVariable int requests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            userService.isUserOnline(userId);
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("requests", requests);
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) requests);
        result.put("method", "isUserOnline");

        return ResponseEntity.ok(result);
    }

    // ===================  ПОСТЫ  ===================

    // GET /api/cache/load-test-post/1/10000
    @GetMapping("/load-test-post/{id}/{requests}")
    public ResponseEntity<Map<String, Object>> loadTestPost(
            @PathVariable Long id,
            @PathVariable int requests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            postService.getPostById(id);
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("postId", id);
        result.put("requests", requests);
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) requests);
        result.put("method", "getPostById");

        return ResponseEntity.ok(result);
    }

    // GET /api/cache/load-test-user-posts/1/5000
    @GetMapping("/load-test-user-posts/{userId}/{requests}")
    public ResponseEntity<Map<String, Object>> loadTestUserPosts(
            @PathVariable Long userId,
            @PathVariable int requests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            postService.getUserPosts(userId, 0, 10); // page=0, size=10
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("requests", requests);
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) requests);
        result.put("method", "getUserPosts");

        return ResponseEntity.ok(result);
    }

    // GET /api/cache/load-test-user-pages/1/1000
    @GetMapping("/load-test-user-pages/{userId}/{requests}")
    public ResponseEntity<Map<String, Object>> loadTestUserPostsPages(
            @PathVariable Long userId,
            @PathVariable int requests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            int page = i % 3; // Тестируем страницы 0, 1, 2
            postService.getUserPosts(userId, page, 10);
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("requests", requests);
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) requests);
        result.put("method", "getUserPosts (pages 0-2)");
        result.put("note", "Тестировались страницы 0, 1, 2 циклично");

        return ResponseEntity.ok(result);
    }

    // GET /api/cache/load-test-multi-users/5000
    @GetMapping("/load-test-multi-users/{requests}")
    public ResponseEntity<Map<String, Object>> loadTestMultiUsers(
            @PathVariable int requests) {

        Long[] userIds = {1L, 2L, 3L, 4L, 5L}; // ID существующих пользователей

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            Long userId = userIds[i % userIds.length]; // Циклично по пользователям
            postService.getUserPosts(userId, 0, 10);
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("userIds", Arrays.toString(userIds));
        result.put("requests", requests);
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) requests);
        result.put("method", "getUserPosts (multiple users)");

        return ResponseEntity.ok(result);
    }

    // ===================  АНАЛИТИКА  ===================

    // GET /api/cache/load-test-analytics/1000
    @GetMapping("/load-test-analytics/{requests}")
    public ResponseEntity<Map<String, Object>> loadTestAnalytics(@PathVariable int requests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            userService.getTotalUsersCount();
            userService.getOnlineUsersCount();
            userService.getUsersCountByDate(LocalDate.now());
            userService.getActiveUsersCountByDate(LocalDate.now());
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("requests", requests * 4); // 4 метода на итерацию
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) (requests * 4));
        result.put("method", "analytics-combo");

        return ResponseEntity.ok(result);
    }

    // ===================  МОНИТОРИНГ  ===================

    // GET /api/cache/stats
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        if (cacheManager instanceof CaffeineCacheManager caffeineCacheManager) {
            caffeineCacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = caffeineCacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache caffeineCache) {
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                            caffeineCache.getNativeCache();

                    CacheStats cacheStats = nativeCache.stats();

                    Map<String, Object> cacheInfo = new HashMap<>();
                    cacheInfo.put("size", nativeCache.estimatedSize());
                    cacheInfo.put("hitCount", cacheStats.hitCount());
                    cacheInfo.put("missCount", cacheStats.missCount());
                    cacheInfo.put("requestCount", cacheStats.requestCount());
                    cacheInfo.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));
                    cacheInfo.put("evictionCount", cacheStats.evictionCount());
                    cacheInfo.put("loadCount", cacheStats.loadCount());

                    stats.put(cacheName, cacheInfo);
                }
            });
        }

        return stats;
    }

    // POST /api/cache/clear/users
    @PostMapping("/clear/{cacheName}")
    public String clearCache(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return "Cache '" + cacheName + "' cleared successfully";
        }
        return "Cache '" + cacheName + "' not found";
    }

    // POST /api/cache/clear-all
    @PostMapping("/clear-all")
    public String clearAllCaches() {
        if (cacheManager instanceof CaffeineCacheManager caffeineCacheManager) {
            caffeineCacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = caffeineCacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
            return "All caches cleared successfully";
        }
        return "Could not clear caches";
    }
}