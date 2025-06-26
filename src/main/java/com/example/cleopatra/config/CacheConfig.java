package com.example.cleopatra.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // =================== КЭШИ ДЛЯ ПОСТОВ ===================

        // Кеш для отдельных постов
        cacheManager.registerCustomCache("posts",
                Caffeine.newBuilder()
                        .maximumSize(1000)                    // Максимум 1000 постов в кеше
                        .expireAfterWrite(15, TimeUnit.MINUTES)  // Истекает через 15 минут после записи
                        .expireAfterAccess(5, TimeUnit.MINUTES)  // Истекает через 5 минут после последнего доступа
                        .recordStats()                           // Включаем статистику кеша
                        .build());

        // Кеш для списков постов пользователей
        cacheManager.registerCustomCache("user-posts",
                Caffeine.newBuilder()
                        .maximumSize(500)                     // Меньше списков, но они больше по размеру
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .expireAfterAccess(3, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // =================== КЭШИ ДЛЯ НОВОСТЕЙ ===================

        // Кеш для отдельных новостей по ID
        cacheManager.registerCustomCache("newsById",
                Caffeine.newBuilder()
                        .maximumSize(2000)                    // Больше новостей, чем постов
                        .expireAfterWrite(30, TimeUnit.MINUTES)  // Новости живут дольше
                        .expireAfterAccess(10, TimeUnit.MINUTES) // Популярные новости остаются в кеше
                        .recordStats()
                        .build());

        // Кеш для страниц опубликованных новостей
        cacheManager.registerCustomCache("publishedNews",
                Caffeine.newBuilder()
                        .maximumSize(1000)                    // Много страниц с разными фильтрами
                        .expireAfterWrite(20, TimeUnit.MINUTES)  // Списки новостей обновляются чаще
                        .expireAfterAccess(5, TimeUnit.MINUTES)  // Быстро устаревают без обращений
                        .recordStats()
                        .build());

        // Кеш для черновиков новостей
        cacheManager.registerCustomCache("news-drafts",
                Caffeine.newBuilder()
                        .maximumSize(500)                     // Меньше черновиков
                        .expireAfterWrite(60, TimeUnit.MINUTES)  // Черновики живут дольше
                        .expireAfterAccess(15, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для статистики новостей
        cacheManager.registerCustomCache("news-stats",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // =================== КЭШИ ДЛЯ ФОРУМА ===================

        // Кеш для детального просмотра тем форума
        cacheManager.registerCustomCache("forums-detailed",
                Caffeine.newBuilder()
                        .maximumSize(2000)                    // Много тем форума
                        .expireAfterWrite(25, TimeUnit.MINUTES)  // Темы живут долго
                        .expireAfterAccess(10, TimeUnit.MINUTES) // Популярные темы остаются в кеше
                        .recordStats()
                        .build());

        // Кеш для страниц списка тем форума (с пагинацией и фильтрами)
        cacheManager.registerCustomCache("forum-pages",
                Caffeine.newBuilder()
                        .maximumSize(1500)                    // Много комбинаций фильтров и страниц
                        .expireAfterWrite(15, TimeUnit.MINUTES)  // Списки обновляются при новых темах
                        .expireAfterAccess(5, TimeUnit.MINUTES)  // Быстро устаревают без обращений
                        .recordStats()
                        .build());

        // Кеш для результатов поиска по форуму
        cacheManager.registerCustomCache("forum-search",
                Caffeine.newBuilder()
                        .maximumSize(1000)                    // Много различных поисковых запросов
                        .expireAfterWrite(10, TimeUnit.MINUTES)  // Результаты поиска быстро устаревают
                        .expireAfterAccess(3, TimeUnit.MINUTES)  // Редко повторяющиеся запросы
                        .recordStats()
                        .build());

        // Кеш для проверки существования тем форума
        cacheManager.registerCustomCache("forum-exists",
                Caffeine.newBuilder()
                        .maximumSize(5000)                    // Легкие булевы значения, можем много
                        .expireAfterWrite(30, TimeUnit.MINUTES)  // Существование темы меняется редко
                        .expireAfterAccess(15, TimeUnit.MINUTES) // Долго актуальны
                        .recordStats()
                        .build());

        // Кеш для счетчиков тем по типам
        cacheManager.registerCustomCache("forum-count",
                Caffeine.newBuilder()
                        .maximumSize(100)                     // Мало типов форума
                        .expireAfterWrite(20, TimeUnit.MINUTES)  // Счетчики обновляются при создании/удалении
                        .expireAfterAccess(10, TimeUnit.MINUTES) // Статистика часто запрашивается
                        .recordStats()
                        .build());

        // Кеш для комментариев к темам форума
        cacheManager.registerCustomCache("forum-comments",
                Caffeine.newBuilder()
                        .maximumSize(3000)                    // Много комментариев
                        .expireAfterWrite(20, TimeUnit.MINUTES)
                        .expireAfterAccess(8, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // =================== КЭШИ ДЛЯ ГРУПП ===================

        // Кеш для отдельных групп
        cacheManager.registerCustomCache("groups",
                Caffeine.newBuilder()
                        .maximumSize(2000)                    // Максимум 2000 групп в кеше
                        .expireAfterWrite(30, TimeUnit.MINUTES)  // Истекает через 30 минут после записи
                        .expireAfterAccess(10, TimeUnit.MINUTES) // Истекает через 10 минут после последнего доступа
                        .recordStats()                           // Включаем статистику кеша
                        .build());

        // Кеш для списков постов групп
        cacheManager.registerCustomCache("group-posts",
                Caffeine.newBuilder()
                        .maximumSize(1000)                    // Максимум 1000 списков постов групп
                        .expireAfterWrite(5, TimeUnit.MINUTES)   // Короткое время жизни - посты часто обновляются
                        .expireAfterAccess(2, TimeUnit.MINUTES)  // Быстрое истечение после доступа
                        .recordStats()
                        .build());

        // Кеш для счетчиков групп (количество постов, участников и т.д.)
        cacheManager.registerCustomCache("group-stats",
                Caffeine.newBuilder()
                        .maximumSize(5000)                    // Много групп могут иметь статистику
                        .expireAfterWrite(20, TimeUnit.MINUTES)  // Умеренное время жизни
                        .expireAfterAccess(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для участников групп
        cacheManager.registerCustomCache("group-members",
                Caffeine.newBuilder()
                        .maximumSize(1000)                    // Списки участников групп
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .expireAfterAccess(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для поиска групп
        cacheManager.registerCustomCache("group-search",
                Caffeine.newBuilder()
                        .maximumSize(1000)                    // Результаты поиска групп
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .expireAfterAccess(3, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для рекомендаций групп
        cacheManager.registerCustomCache("group-recommendations",
                Caffeine.newBuilder()
                        .maximumSize(500)                     // Персональные рекомендации
                        .expireAfterWrite(60, TimeUnit.MINUTES)  // Долгое время жизни
                        .expireAfterAccess(15, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // =================== КЭШИ ДЛЯ ПОЛЬЗОВАТЕЛЕЙ ===================

        // Кеш для профилей пользователей
        cacheManager.registerCustomCache("user-profiles",
                Caffeine.newBuilder()
                        .maximumSize(3000)                    // Много пользователей
                        .expireAfterWrite(45, TimeUnit.MINUTES)  // Профили меняются редко
                        .expireAfterAccess(15, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для настроек пользователей
        cacheManager.registerCustomCache("user-settings",
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(60, TimeUnit.MINUTES)  // Настройки меняются очень редко
                        .expireAfterAccess(20, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для друзей/подписчиков пользователей
        cacheManager.registerCustomCache("user-connections",
                Caffeine.newBuilder()
                        .maximumSize(1500)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // =================== КЭШИ ДЛЯ УВЕДОМЛЕНИЙ ===================

        // Кеш для непрочитанных уведомлений
        cacheManager.registerCustomCache("notifications-unread",
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)   // Быстро обновляются
                        .expireAfterAccess(2, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для истории уведомлений
        cacheManager.registerCustomCache("notifications-history",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // =================== КЭШИ ДЛЯ СИСТЕМЫ ===================

        // Кеш для системных настроек
        cacheManager.registerCustomCache("system-config",
                Caffeine.newBuilder()
                        .maximumSize(100)                     // Мало системных настроек
                        .expireAfterWrite(120, TimeUnit.MINUTES) // Очень долго живут
                        .expireAfterAccess(60, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для статистики системы
        cacheManager.registerCustomCache("system-stats",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(15, TimeUnit.MINUTES)  // Статистика обновляется часто
                        .expireAfterAccess(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Кеш для результатов глобального поиска
        cacheManager.registerCustomCache("global-search",
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(10, TimeUnit.MINUTES)  // Поиск быстро устаревает
                        .expireAfterAccess(3, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        return cacheManager;
    }


    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("❌ Ошибка при получении из кеша '{}' ключа '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("❌ Ошибка при записи в кеш '{}' ключа '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("❌ Ошибка при удалении из кеша '{}' ключа '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("❌ Ошибка при очистке кеша '{}': {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }



//
//    /**
//     * Планировщик для логирования статистики кеша
//     */
//    @Scheduled(fixedRate = 300000) // Каждые 5 минут
//    public void logCacheStatistics() {
//        if (log.isInfoEnabled()) {
//            CacheManager cm = cacheManager();
//            cm.getCacheNames().forEach(cacheName -> {
//                Cache cache = cm.getCache(cacheName);
//                if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
//                    com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
//                            (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();
//
//                    var stats = caffeineCache.stats();
//                    log.info("📊 Статистика кеша '{}': Размер={}, Попадания={}, Промахи={}, Hit Rate={:.2f}%",
//                            cacheName,
//                            caffeineCache.estimatedSize(),
//                            stats.hitCount(),
//                            stats.missCount(),
//                            stats.hitRate() * 100
//                    );
//                }
//            });
//        }
//    }

    /**
     * Планировщик для периодической очистки просроченных элементов кеша
     */
    @Scheduled(cron = "0 0 2 * * *") // Каждый день в 2:00
    public void cleanupExpiredCacheEntries() {
        log.info("🧹 Запуск очистки просроченных элементов кеша");

        CacheManager cm = cacheManager();
        cm.getCacheNames().forEach(cacheName -> {
            Cache cache = cm.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                        (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();

                long sizeBefore = caffeineCache.estimatedSize();
                caffeineCache.cleanUp(); // Принудительная очистка просроченных элементов
                long sizeAfter = caffeineCache.estimatedSize();

                if (sizeBefore != sizeAfter) {
                    log.info("🧹 Кеш '{}': очищено {} просроченных элементов (было: {}, стало: {})",
                            cacheName, sizeBefore - sizeAfter, sizeBefore, sizeAfter);
                }
            }
        });

        log.info("✅ Очистка просроченных элементов кеша завершена");
    }

    /**
     * Получение детальной статистики всех кешей
     */
    public Map<String, Map<String, Object>> getDetailedCacheStatistics() {
        Map<String, Map<String, Object>> allStats = new HashMap<>();

        CacheManager cm = cacheManager();
        cm.getCacheNames().forEach(cacheName -> {
            Cache cache = cm.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                        (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();

                var stats = caffeineCache.stats();
                Map<String, Object> cacheStats = new HashMap<>();

                cacheStats.put("estimatedSize", caffeineCache.estimatedSize());
                cacheStats.put("hitCount", stats.hitCount());
                cacheStats.put("missCount", stats.missCount());
                cacheStats.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
                cacheStats.put("missRate", String.format("%.2f%%", stats.missRate() * 100));
                cacheStats.put("evictionCount", stats.evictionCount());
                cacheStats.put("requestCount", stats.requestCount());
                cacheStats.put("averageLoadTime", String.format("%.2f ms", stats.averageLoadPenalty() / 1_000_000.0));
                cacheStats.put("loadCount", stats.loadCount());


                allStats.put(cacheName, cacheStats);
            }
        });

        return allStats;
    }

    /**
     * Очистка всех кешей (для административных целей)
     */
    public void clearAllCaches() {
        log.warn("⚠️ Принудительная очистка всех кешей");

        CacheManager cm = cacheManager();
        cm.getCacheNames().forEach(cacheName -> {
            Cache cache = cm.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("🗑️ Кеш '{}' очищен", cacheName);
            }
        });

        log.warn("✅ Все кеши очищены");
    }

    /**
     * Получение статистики конкретного кеша
     */
    public Map<String, Object> getCacheStatistics(String cacheName) {
        CacheManager cm = cacheManager();
        Cache cache = cm.getCache(cacheName);

        if (cache == null) {
            throw new IllegalArgumentException("Кеш с именем '" + cacheName + "' не найден");
        }

        if (cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();

            var stats = caffeineCache.stats();
            Map<String, Object> cacheStats = new HashMap<>();

            cacheStats.put("cacheName", cacheName);
            cacheStats.put("estimatedSize", caffeineCache.estimatedSize());
            cacheStats.put("hitCount", stats.hitCount());
            cacheStats.put("missCount", stats.missCount());
            cacheStats.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
            cacheStats.put("missRate", String.format("%.2f%%", stats.missRate() * 100));
            cacheStats.put("evictionCount", stats.evictionCount());
            cacheStats.put("requestCount", stats.requestCount());
            cacheStats.put("averageLoadTime", String.format("%.2f ms", stats.averageLoadPenalty() / 1_000_000.0));
            cacheStats.put("loadCount", stats.loadCount());

            return cacheStats;
        }

        throw new IllegalStateException("Кеш '" + cacheName + "' не является Caffeine кешем");
    }
}