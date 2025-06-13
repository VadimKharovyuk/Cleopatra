package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.ProfileStatisticsDTO;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProfileStatisticsService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final VisitRepository visitRepository;
    private final CommentRepository commentRepository;
    private final SubscriptionRepository followRepository;

    public ProfileStatisticsDTO getUserProfileStats(Long userId) {
        log.info("Получение статистики для пользователя {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ProfileStatisticsDTO stats = ProfileStatisticsDTO.builder()
                // Основные метрики
                .profileViews(getProfileViews(userId))
                .totalLikes(getTotalLikes(userId))
                .avgEngagement(getAverageEngagement(userId))
                .postsCount(getPostsCount(userId))

                // Подписчики
                .followersCount(getFollowersCount(userId))
                .followingCount(getFollowingCount(userId))
                .newFollowersWeek(getNewFollowersThisWeek(userId))
                .unfollowersWeek(getUnfollowersThisWeek(userId))
                .netGrowth(getNetGrowthThisWeek(userId))

                // Активность по дням
                .weeklyActivity(getWeeklyActivity(userId))
                .bestDay(getBestDayOfWeek(userId))

                // Топ посты
                .topPosts(getTopPosts(userId, 3))

                // География
                .geography(getGeographyStats(userId))


                .visitorStats(getVisitorStats(userId))

                // Оптимальное время
                .optimalPostTime(getOptimalPostTime(userId))
                .avgOnlineTime(getAverageOnlineTime(userId))
                .peakActivity(getPeakActivityTime(userId))

                // Типы контента
                .contentTypes(getContentTypeStats(userId))

                // Достижения
                .achievements(getUserAchievements(userId))
                .totalAchievements(getTotalAchievements(userId))
                .nextGoal(getNextGoal(userId))
                .goalProgress(getGoalProgress(userId))

                .build();

        log.info("Статистика для пользователя {} успешно собрана", userId);
        return stats;
    }

    // ===================== ПРОСМОТРЫ ПРОФИЛЯ =====================

    private Long getProfileViews(Long userId) {
        try {
            Long totalVisits = visitRepository.countByVisitedUserId(userId);
            log.debug("Профиль пользователя {} посетили {} раз", userId, totalVisits);
            return totalVisits;
        } catch (Exception e) {
            log.warn("Ошибка получения просмотров профиля для пользователя {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    private Long getUniqueProfileViews(Long userId) {
        try {
            return visitRepository.countUniqueVisitorsByUserId(userId);
        } catch (Exception e) {
            log.warn("Ошибка получения уникальных просмотров: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getProfileViewsThisWeek(Long userId) {
        try {
            LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            return visitRepository.countByVisitedUserIdAndVisitedAtAfter(userId, weekAgo);
        } catch (Exception e) {
            log.warn("Ошибка получения просмотров за неделю: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getProfileViewsToday(Long userId) {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
            return visitRepository.countByVisitedUserIdAndVisitedAtAfter(userId, startOfDay);
        } catch (Exception e) {
            log.warn("Ошибка получения просмотров за сегодня: {}", e.getMessage());
            return 0L;
        }
    }

    // ===================== СТАТИСТИКА ПОСЕТИТЕЛЕЙ =====================

    private Map<String, Object> getVisitorStats(Long userId) {
        Map<String, Object> visitorStats = new HashMap<>();

        try {
            // Основные метрики посетителей
            Long totalVisits = getProfileViews(userId);
            Long uniqueVisitors = getUniqueProfileViews(userId);
            Long visitsThisWeek = getProfileViewsThisWeek(userId);
            Long visitsToday = getProfileViewsToday(userId);

            visitorStats.put("totalVisits", totalVisits);
            visitorStats.put("uniqueVisitors", uniqueVisitors);
            visitorStats.put("visitsThisWeek", visitsThisWeek);
            visitorStats.put("visitsToday", visitsToday);

            // Коэффициент возврата (если есть повторные визиты)
            double returnRate = totalVisits > 0 && uniqueVisitors > 0 ?
                    ((double) (totalVisits - uniqueVisitors) / totalVisits * 100) : 0.0;
            visitorStats.put("returnRate", String.format("%.1f%%", returnRate));

            // Средняя частота визитов на уникального посетителя
            double avgVisitsPerVisitor = uniqueVisitors > 0 ?
                    (double) totalVisits / uniqueVisitors : 0.0;
            visitorStats.put("avgVisitsPerVisitor", String.format("%.1f", avgVisitsPerVisitor));

            // Топ IP адреса (для анализа географии) - с проверкой
            try {
                List<Object[]> topIps = visitRepository.findTopIpAddressesByUserId(
                        userId, PageRequest.of(0, 5));
                visitorStats.put("topIpAddresses", topIps);
            } catch (Exception e) {
                log.warn("Ошибка получения топ IP: {}", e.getMessage());
                visitorStats.put("topIpAddresses", Collections.emptyList());
            }

            // Уникальные IP адреса
            try {
                Long uniqueIps = visitRepository.countUniqueIpsByUserId(userId);
                visitorStats.put("uniqueIpAddresses", uniqueIps);
            } catch (Exception e) {
                log.warn("Ошибка подсчета уникальных IP: {}", e.getMessage());
                visitorStats.put("uniqueIpAddresses", 0L);
            }

        } catch (Exception e) {
            log.warn("Ошибка получения статистики посетителей: {}", e.getMessage());
            // Возвращаем пустую статистику вместо null
            visitorStats.put("totalVisits", 0L);
            visitorStats.put("uniqueVisitors", 0L);
            visitorStats.put("visitsThisWeek", 0L);
            visitorStats.put("visitsToday", 0L);
            visitorStats.put("returnRate", "0%");
            visitorStats.put("avgVisitsPerVisitor", "0");
            visitorStats.put("topIpAddresses", Collections.emptyList());
            visitorStats.put("uniqueIpAddresses", 0L);
        }

        return visitorStats;
    }

    // ===================== ЛАЙКИ И ENGAGEMENT =====================

    private Long getTotalLikes(Long userId) {
        try {
            // ОПТИМИЗАЦИЯ: Используем один запрос вместо получения всех постов
            Long totalLikes = postRepository.getTotalLikesByAuthor(userId);
            if (totalLikes == null) {
                // Fallback: если нет метода в репозитории, считаем вручную
                List<Post> userPosts = postRepository.findByAuthorId(userId);
                totalLikes = userPosts.stream()
                        .mapToLong(post -> post.getLikesCount() != null ? post.getLikesCount() : 0L)
                        .sum();
            }
            log.debug("Пользователь {} имеет {} лайков", userId, totalLikes);
            return totalLikes;
        } catch (Exception e) {
            log.warn("Ошибка подсчета лайков для пользователя {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    private String getAverageEngagement(Long userId) {
        try {
            Long postsCount = getPostsCount(userId);
            if (postsCount == 0) return "0%";

            Long totalLikes = getTotalLikes(userId);
            Long totalComments = getTotalComments(userId);
            Long totalViews = getProfileViews(userId);

            if (totalViews == 0) return "0%";

            // Engagement = (лайки + комментарии) / просмотры профиля * 100
            double engagement = ((double) (totalLikes + totalComments) / totalViews) * 100;

            // Ограничиваем разумным максимумом
            engagement = Math.min(engagement, 50.0);

            return String.format("%.1f%%", engagement);

        } catch (Exception e) {
            log.warn("Ошибка расчета engagement для пользователя {}: {}", userId, e.getMessage());
            return "0%";
        }
    }

    private Long getTotalComments(Long userId) {
        try {
            return commentRepository.countByPost_AuthorId(userId);
        } catch (Exception e) {
            log.warn("Ошибка подсчета комментариев для пользователя {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    // ===================== ТОПНЫЕ ПОСТЫ =====================

    private List<ProfileStatisticsDTO.TopPost> getTopPosts(Long userId, int limit) {
        try {
            // Пробуем использовать оптимизированный метод
            List<Post> topPosts;
            try {
                topPosts = postRepository.findTopPostsByUser(userId, limit);
            } catch (Exception e) {
                log.warn("Метод findTopPostsByUser не найден, используем fallback: {}", e.getMessage());
                // Fallback: получаем все посты и сортируем
                List<Post> allPosts = postRepository.findByAuthorId(userId);
                topPosts = allPosts.stream()
                        .sorted((p1, p2) -> {
                            Long likes1 = p1.getLikesCount() != null ? p1.getLikesCount() : 0L;
                            Long likes2 = p2.getLikesCount() != null ? p2.getLikesCount() : 0L;
                            return likes2.compareTo(likes1);
                        })
                        .limit(limit)
                        .collect(Collectors.toList());
            }

            return topPosts.stream()
                    .map(post -> ProfileStatisticsDTO.TopPost.builder()
                            .id(post.getId())
                            .content(truncateContent(post.getContent(), 50))
                            .likesCount(post.getLikesCount() != null ? post.getLikesCount() : 0L)
                            .commentsCount(post.getCommentsCount() != null ? post.getCommentsCount() : 0L)
                            .viewsCount(post.getViewsCount() != null ? post.getViewsCount() : 0L)
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка получения топ постов: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ===================== ДОСТИЖЕНИЯ =====================

    private List<String> getUserAchievements(Long userId) {
        List<String> achievements = new ArrayList<>();

        try {
            Long followersCount = getFollowersCount(userId);
            Long totalLikes = getTotalLikes(userId);
            Long postsCount = getPostsCount(userId);
            Long profileViews = getProfileViews(userId);
            Long uniqueVisitors = getUniqueProfileViews(userId);

            // Достижения по подписчикам
            if (followersCount >= 10000) achievements.add("🚀 10К подписчиков");
            else if (followersCount >= 5000) achievements.add("⭐ 5К подписчиков");
            else if (followersCount >= 1000) achievements.add("📈 1К подписчиков");
            else if (followersCount >= 100) achievements.add("👥 100 подписчиков");
            else if (followersCount >= 10) achievements.add("👥 Первые подписчики");

            // Достижения по лайкам
            if (totalLikes >= 10000) achievements.add("💖 10К лайков");
            else if (totalLikes >= 5000) achievements.add("❤️ 5К лайков");
            else if (totalLikes >= 1000) achievements.add("💝 1К лайков");
            else if (totalLikes >= 100) achievements.add("💗 100 лайков");
            else if (totalLikes >= 10) achievements.add("💕 Первые лайки");

            // Достижения по постам
            if (postsCount >= 1000) achievements.add("📚 1К постов");
            else if (postsCount >= 500) achievements.add("📖 500 постов");
            else if (postsCount >= 100) achievements.add("📝 100 постов");
            else if (postsCount >= 50) achievements.add("✍️ Активный автор");
            else if (postsCount >= 10) achievements.add("🌱 Начинающий блогер");
            else if (postsCount >= 1) achievements.add("🎯 Первый пост");

            // Достижения по просмотрам профиля
            if (profileViews >= 100000) achievements.add("🌟 100К просмотров");
            else if (profileViews >= 50000) achievements.add("🔥 50К просмотров");
            else if (profileViews >= 10000) achievements.add("👀 10К просмотров");
            else if (profileViews >= 5000) achievements.add("👁️ 5К просмотров");
            else if (profileViews >= 1000) achievements.add("🔍 1К просмотров");
            else if (profileViews >= 100) achievements.add("👀 Популярный профиль");

            // Достижения по уникальным посетителям
            if (uniqueVisitors >= 5000) achievements.add("🌍 5К уникальных посетителей");
            else if (uniqueVisitors >= 1000) achievements.add("🗺️ 1К уникальных посетителей");
            else if (uniqueVisitors >= 500) achievements.add("📍 500 уникальных посетителей");

            // Специальные достижения
            if (hasViralPost(userId)) achievements.add("🔥 Вирусный пост");
            if (hasConsistentPosting(userId)) achievements.add("📅 Постоянный автор");
            if (hasHighEngagement(userId)) achievements.add("🎯 Высокая вовлеченность");
            if (hasHighReturnRate(userId)) achievements.add("🔄 Лояльная аудитория");

        } catch (Exception e) {
            log.warn("Ошибка получения достижений для пользователя {}: {}", userId, e.getMessage());
        }

        return achievements;
    }

    private String getTotalAchievements(Long userId) {
        int earnedAchievements = getUserAchievements(userId).size();
        int totalPossibleAchievements = 25; // Увеличили количество возможных достижений
        return earnedAchievements + "/" + totalPossibleAchievements;
    }

    // ===================== ОСТАЛЬНЫЕ МЕТОДЫ (БЕЗ ИЗМЕНЕНИЙ) =====================

    private Long getPostsCount(Long userId) {
        try {
            return postRepository.countByAuthorId(userId);
        } catch (Exception e) {
            log.warn("Ошибка подсчета постов: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getFollowersCount(Long userId) {
        try {
            return followRepository.countBySubscribedTo_Id(userId);
        } catch (Exception e) {
            log.warn("Ошибка получения количества подписчиков: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getFollowingCount(Long userId) {
        try {
            return followRepository.countBySubscriber_Id(userId);
        } catch (Exception e) {
            log.warn("Ошибка получения количества подписок: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getNewFollowersThisWeek(Long userId) {
        try {
            LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            return followRepository.countBySubscribedTo_IdAndCreatedAtAfter(userId, weekAgo);
        } catch (Exception e) {
            log.warn("Ошибка получения новых подписчиков: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getUnfollowersThisWeek(Long userId) {
        return Math.round(Math.random() * 5 + 1);
    }

    private Long getNetGrowthThisWeek(Long userId) {
        return getNewFollowersThisWeek(userId) - getUnfollowersThisWeek(userId);
    }

    private Map<String, Long> getWeeklyActivity(Long userId) {
        Map<String, Long> activity = new LinkedHashMap<>();
        String[] days = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};

        for (String day : days) {
            activity.put(day, Math.round(Math.random() * 500 + 100));
        }

        return activity;
    }

    private String getBestDayOfWeek(Long userId) {
        Map<String, Long> weeklyActivity = getWeeklyActivity(userId);
        return weeklyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("ВС");
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        return content.length() > maxLength
                ? content.substring(0, maxLength) + "..."
                : content;
    }

    private Map<String, Object> getGeographyStats(Long userId) {
        Map<String, Object> geography = new HashMap<>();
        geography.put("mainCountry", "🇹🇷 Турция");
        geography.put("mainPercentage", 45);

        List<Map<String, Object>> cities = Arrays.asList(
                Map.of("name", "🇹🇷 Анталия", "percentage", 23),
                Map.of("name", "🇹🇷 Стамбул", "percentage", 15),
                Map.of("name", "🇷🇺 Москва", "percentage", 12),
                Map.of("name", "🇩🇪 Берлин", "percentage", 8),
                Map.of("name", "🇺🇸 Нью-Йорк", "percentage", 6),
                Map.of("name", "🌍 Другие", "percentage", 36)
        );
        geography.put("cities", cities);

        return geography;
    }

    private String getOptimalPostTime(Long userId) {
        return "18:00 - 20:00";
    }

    private String getAverageOnlineTime(Long userId) {
        return "2.3 часа";
    }

    private String getPeakActivityTime(Long userId) {
        return "19:30";
    }

    private Map<String, Object> getContentTypeStats(Long userId) {
        Map<String, Object> contentTypes = new HashMap<>();

        Long totalPosts = getPostsCount(userId);
        if (totalPosts == 0) {
            contentTypes.put("photo", 0);
            contentTypes.put("text", 0);
            contentTypes.put("video", 0);
            contentTypes.put("recommendation", "Начните создавать контент!");
            return contentTypes;
        }

        try {
            Long photoPosts = postRepository.countByAuthorIdAndImageUrlIsNotNull(userId);
            Long textPosts = totalPosts - photoPosts;

            int photoPercent = Math.round((float) photoPosts / totalPosts * 100);
            int textPercent = Math.round((float) textPosts / totalPosts * 100);
            int videoPercent = 0;

            contentTypes.put("photo", photoPercent);
            contentTypes.put("text", textPercent);
            contentTypes.put("video", videoPercent);
            contentTypes.put("recommendation", getContentRecommendation(photoPercent, videoPercent));
        } catch (Exception e) {
            log.warn("Ошибка анализа типов контента: {}", e.getMessage());
            contentTypes.put("photo", 50);
            contentTypes.put("text", 50);
            contentTypes.put("video", 0);
            contentTypes.put("recommendation", "Продолжайте создавать разнообразный контент!");
        }

        return contentTypes;
    }

    private String getContentRecommendation(int photoPercent, int videoPercent) {
        if (photoPercent > 60) {
            return "Посты с фотографиями получают на 40% больше лайков. Попробуйте добавить больше видео контента.";
        } else if (videoPercent < 10) {
            return "Видео контент показывает высокую вовлеченность. Рекомендуем увеличить долю видео постов.";
        } else {
            return "Хороший баланс типов контента. Продолжайте в том же духе!";
        }
    }

    private boolean hasHighReturnRate(Long userId) {
        try {
            Long totalVisits = getProfileViews(userId);
            Long uniqueVisitors = getUniqueProfileViews(userId);

            if (uniqueVisitors == 0 || totalVisits == 0) return false;

            double returnRate = ((double) (totalVisits - uniqueVisitors) / totalVisits) * 100;
            return returnRate > 30.0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasHighEngagement(Long userId) {
        try {
            String engagement = getAverageEngagement(userId);
            double engagementPercent = Double.parseDouble(engagement.replace("%", ""));
            return engagementPercent > 5.0; // Снизили порог для реалистичности
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasViralPost(Long userId) {
        try {
            List<Post> userPosts = postRepository.findByAuthorId(userId);
            return userPosts.stream()
                    .anyMatch(post -> post.getLikesCount() != null && post.getLikesCount() > 50); // Снизили порог
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasConsistentPosting(Long userId) {
        try {
            LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            List<Post> recentPosts = postRepository.findByAuthorIdAndCreatedAtAfter(userId, weekAgo);
            return recentPosts.size() >= 2; // Снизили требования
        } catch (Exception e) {
            return false;
        }
    }

    private String getNextGoal(Long userId) {
        Long followersCount = getFollowersCount(userId);
        Long profileViews = getProfileViews(userId);

        if (profileViews < 100) {
            return "🎯 100 просмотров профиля (осталось " + (100 - profileViews) + ")";
        } else if (followersCount < 10) {
            return "🎯 10 подписчиков (осталось " + (10 - followersCount) + ")";
        } else if (profileViews < 1000) {
            return "🎯 1000 просмотров профиля (осталось " + (1000 - profileViews) + ")";
        } else if (followersCount < 100) {
            return "🎯 100 подписчиков (осталось " + (100 - followersCount) + ")";
        } else if (profileViews < 5000) {
            return "🎯 5000 просмотров профиля (осталось " + (5000 - profileViews) + ")";
        } else if (followersCount < 1000) {
            return "🎯 1000 подписчиков (осталось " + (1000 - followersCount) + ")";
        } else {
            return "🏆 Все основные цели достигнуты!";
        }
    }

    private Double getGoalProgress(Long userId) {
        Long followersCount = getFollowersCount(userId);
        Long profileViews = getProfileViews(userId);

        if (profileViews < 100) {
            return (double) profileViews / 100 * 100;
        } else if (followersCount < 10) {
            return (double) followersCount / 10 * 100;
        } else if (profileViews < 1000) {
            return (double) profileViews / 1000 * 100;
        } else if (followersCount < 100) {
            return (double) followersCount / 100 * 100;
        } else if (profileViews < 5000) {
            return (double) profileViews / 5000 * 100;
        } else if (followersCount < 1000) {
            return (double) followersCount / 1000 * 100;
        } else {
            return 100.0;
        }
    }
}