package com.example.cleopatra.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {


    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

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


        // Кеш для статистики постов
        cacheManager.registerCustomCache("post-stats",
                Caffeine.newBuilder()
                        .maximumSize(100)                     // Мало статистических данных
                        .expireAfterWrite(30, TimeUnit.MINUTES) // Статистика может жить дольше
                        .recordStats()
                        .build());





        // ✅ ОПТИМИЗИРОВАННАЯ КОНФИГУРАЦИЯ КЕША

// Основные пользователи - долгоживущий кеш
        cacheManager.registerCustomCache("users",
                Caffeine.newBuilder()
                        .maximumSize(1500)                      // ⬆️ Увеличили с 1000
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .expireAfterAccess(5, TimeUnit.MINUTES) // ✅ ДОБАВИЛИ для неактивных
                        .recordStats()
                        .removalListener((key, value, cause) ->
                                log.debug("👤 Cache [users] evicted key: {}, cause: {}", key, cause))
                        .build());

// Email поиск - средне долгоживущий
        cacheManager.registerCustomCache("users-by-email",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .recordStats()
                        .removalListener((key, value, cause) ->
                                log.debug("📧 Cache [users-by-email] evicted key: {}, cause: {}", key, cause))
                        .build());

// User entities - активно используемый кеш
        cacheManager.registerCustomCache("user-entities",
                Caffeine.newBuilder()
                        .maximumSize(3000)                      // ⬆️ Увеличили с 2000 (много entity операций)
                        .expireAfterWrite(20, TimeUnit.MINUTES)
                        .expireAfterAccess(8, TimeUnit.MINUTES)
                        .recordStats()
                        .removalListener((key, value, cause) ->
                                log.debug("🏗️ Cache [user-entities] evicted key: {}, cause: {}", key, cause))
                        .build());

// Статус пользователей - короткоживущий кеш
        cacheManager.registerCustomCache("user-status",
                Caffeine.newBuilder()
                        .maximumSize(5000)                      // ⬆️ Увеличили с 2000 (много онлайн проверок)
                        .expireAfterWrite(90, TimeUnit.SECONDS) // ⬇️ Уменьшили с 2 минут (статус меняется быстро)
                        .expireAfterAccess(30, TimeUnit.SECONDS) // ✅ ДОБАВИЛИ для более быстрой инвалидации
                        .recordStats()
                        .removalListener((key, value, cause) ->
                                log.trace("⚡ Cache [user-status] evicted key: {}, cause: {}", key, cause))
                        .build());

// Аналитика - очень долгоживущий кеш
        cacheManager.registerCustomCache("user-analytics",
                Caffeine.newBuilder()
                        .maximumSize(500)                       // ⬆️ Увеличили с 200 (больше аналитических запросов)
                        .expireAfterWrite(2, TimeUnit.HOURS)    // ⬆️ Увеличили с 1 часа (аналитика обновляется редко)
                        .expireAfterAccess(30, TimeUnit.MINUTES) // ✅ ДОБАВИЛИ для освобождения памяти
                        .recordStats()
                        .removalListener((key, value, cause) ->
                                log.debug("📊 Cache [user-analytics] evicted key: {}, cause: {}", key, cause))
                        .build());

// ✅ ДОПОЛНИТЕЛЬНО: Кеш для часто запрашиваемых данных
        cacheManager.registerCustomCache("user-brief",
                Caffeine.newBuilder()
                        .maximumSize(2000)                      // Для convertToUserBriefDto
                        .expireAfterWrite(5, TimeUnit.MINUTES)  // Короткий TTL для чатов
                        .expireAfterAccess(2, TimeUnit.MINUTES)
                        .recordStats()
                        .removalListener((key, value, cause) ->
                                log.debug("💬 Cache [user-brief] evicted key: {}, cause: {}", key, cause))
                        .build());

        return cacheManager;
    }


    ///я ебу блять
}