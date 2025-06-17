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



        return cacheManager;
    }


    ///я ебу блять
}