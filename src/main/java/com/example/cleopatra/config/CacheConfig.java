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

        // =================== СУЩЕСТВУЮЩИЕ КЭШИ ДЛЯ ПОСТОВ ===================

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

        // =================== СУЩЕСТВУЮЩИЕ КЭШИ ДЛЯ НОВОСТЕЙ ===================

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

        // =================== НОВЫЕ КЭШИ ДЛЯ ФОРУМА ===================

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

        return cacheManager;
    }

}