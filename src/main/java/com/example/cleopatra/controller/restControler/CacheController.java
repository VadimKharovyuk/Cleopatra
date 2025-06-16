package com.example.cleopatra.controller.restControler;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private CacheManager cacheManager;


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