package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.service.PostService;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cache")
public class CacheController {
    private final  CacheManager cacheManager;
    private final  PostService postService;



///3. Комплексный тест (несколько пользователей):
/// # Тест множества пользователей
/// GET /api/cache/load-test-multi-users/5000
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


    ///. Расширенный тест с разными страницами:
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



///. Простой тест для одного пользователя:
    @GetMapping("/load-test-user/{userId}/{requests}")
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




    ///посты по Id
    ///
    /// http://localhost:2027/api/cache/load-test/125/10000
    @GetMapping("/load-test/{id}/{requests}")
    public ResponseEntity<Map<String, Object>> loadTest(
            @PathVariable Long id,
            @PathVariable int requests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            postService.getPostById(id);
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("requests", requests);
        result.put("timeMs", endTime - startTime);
        result.put("avgTimePerRequest", (endTime - startTime) / (double) requests);

        return ResponseEntity.ok(result);
    }


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

    @PostMapping("/clear/{cacheName}")
    public String clearCache(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return "Cache '" + cacheName + "' cleared successfully";
        }
        return "Cache '" + cacheName + "' not found";
    }
}