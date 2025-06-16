package com.example.cleopatra.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

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




        // Основные пользователи - долгоживущий кеш
        cacheManager.registerCustomCache("users",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        cacheManager.registerCustomCache("users-by-email",
                Caffeine.newBuilder()
                        .maximumSize(1000)                    // 1000 email поисков
                        .expireAfterWrite(30, TimeUnit.MINUTES)  // Email редко меняется
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build());


        // Статус пользователей - короткоживущий кеш
        cacheManager.registerCustomCache("user-status",
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Аналитика - очень долгоживущий кеш
        cacheManager.registerCustomCache("user-analytics",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .recordStats()
                        .build());

        // Кеш для User entities (не DTO)
        cacheManager.registerCustomCache("user-entities",
                Caffeine.newBuilder()
                        .maximumSize(2000)                    // Больше entities
                        .expireAfterWrite(20, TimeUnit.MINUTES)  // Чуть меньше TTL
                        .expireAfterAccess(8, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        return cacheManager;
    }
}
