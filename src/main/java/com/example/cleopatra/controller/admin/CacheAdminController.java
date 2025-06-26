package com.example.cleopatra.controller.admin;

import com.example.cleopatra.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheAdminController {

    private final CacheConfig cacheConfig;

    @GetMapping("/stats")
    public Map<String, Map<String, Object>> getAllStats() {
        return cacheConfig.getDetailedCacheStatistics();
    }

    @GetMapping("/stats/{cacheName}")
    public Map<String, Object> getCacheStats(@PathVariable String cacheName) {
        return cacheConfig.getCacheStatistics(cacheName);
    }

    @PostMapping("/clear-all")
    public String clearAll() {
        cacheConfig.clearAllCaches();
        return "Все кеши очищены";
    }
}
