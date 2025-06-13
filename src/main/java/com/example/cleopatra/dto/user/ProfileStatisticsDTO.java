package com.example.cleopatra.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStatisticsDTO {

    // Основные метрики
    private Long profileViews;
    private Long totalLikes;
    private String avgEngagement;
    private Long postsCount;

    // Подписчики
    private Long followersCount;
    private Long followingCount;
    private Long newFollowersWeek;
    private Long unfollowersWeek;
    private Long netGrowth;

    // Активность по дням
    private Map<String, Long> weeklyActivity;
    private String bestDay;

    // Топ посты
    private List<TopPost> topPosts;

    // География
    private Map<String, Object> geography;

    // НОВОЕ: Статистика посетителей
    private Map<String, Object> visitorStats;

    // Оптимальное время
    private String optimalPostTime;
    private String avgOnlineTime;
    private String peakActivity;

    // Типы контента
    private Map<String, Object> contentTypes;

    // Достижения
    private List<String> achievements;
    private String totalAchievements;
    private String nextGoal;
    private Double goalProgress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPost {
        private Long id;
        private String content;
        private Long likesCount;
        private Long commentsCount;
        private Long viewsCount;
    }

    // Вспомогательные методы для шаблона
    public String getFormattedProfileViews() {
        return formatNumber(profileViews);
    }

    public String getFormattedTotalLikes() {
        return formatNumber(totalLikes);
    }

    public String getFormattedFollowersCount() {
        return formatNumber(followersCount);
    }

    private String formatNumber(Long number) {
        if (number == null) return "0";

        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return number.toString();
        }
    }

    // НОВЫЕ методы для статистики посетителей
    public String getFormattedUniqueVisitors() {
        if (visitorStats == null) return "0";
        Long uniqueVisitors = (Long) visitorStats.get("uniqueVisitors");
        return formatNumber(uniqueVisitors);
    }

    public String getFormattedVisitsThisWeek() {
        if (visitorStats == null) return "0";
        Long visitsThisWeek = (Long) visitorStats.get("visitsThisWeek");
        return formatNumber(visitsThisWeek);
    }

    public String getFormattedVisitsToday() {
        if (visitorStats == null) return "0";
        Long visitsToday = (Long) visitorStats.get("visitsToday");
        return formatNumber(visitsToday);
    }

    public String getReturnRate() {
        if (visitorStats == null) return "0%";
        return (String) visitorStats.getOrDefault("returnRate", "0%");
    }

    public String getAvgVisitsPerVisitor() {
        if (visitorStats == null) return "0";
        return (String) visitorStats.getOrDefault("avgVisitsPerVisitor", "0");
    }

    public Long getUniqueIpAddresses() {
        if (visitorStats == null) return 0L;
        return (Long) visitorStats.getOrDefault("uniqueIpAddresses", 0L);
    }

    // Существующие методы
    public String getNewFollowersWeekFormatted() {
        return newFollowersWeek != null && newFollowersWeek > 0
                ? "+" + newFollowersWeek
                : String.valueOf(newFollowersWeek);
    }

    public String getUnfollowersWeekFormatted() {
        return unfollowersWeek != null && unfollowersWeek > 0
                ? "-" + unfollowersWeek
                : String.valueOf(unfollowersWeek);
    }

    public String getNetGrowthFormatted() {
        return netGrowth != null && netGrowth > 0
                ? "+" + netGrowth
                : String.valueOf(netGrowth);
    }

    public String getNetGrowthTrendClass() {
        if (netGrowth == null) return "trend-stable";
        return netGrowth > 0 ? "trend-up" : netGrowth < 0 ? "trend-down" : "trend-stable";
    }

    public String getGoalProgressFormatted() {
        return goalProgress != null
                ? String.format("%.1f%%", goalProgress)
                : "0%";
    }

    // Методы для анализа трендов
    public String getProfileViewsTrend() {
        if (visitorStats == null) return "trend-stable";

        Long totalVisits = (Long) visitorStats.get("totalVisits");
        Long visitsThisWeek = (Long) visitorStats.get("visitsThisWeek");

        if (totalVisits == null || visitsThisWeek == null || totalVisits == 0) {
            return "trend-stable";
        }

        // Примерный расчет: если за неделю больше 20% от всех просмотров - растет
        double weeklyPercentage = (double) visitsThisWeek / totalVisits * 100;

        if (weeklyPercentage > 20) return "trend-up";
        else if (weeklyPercentage < 5) return "trend-down";
        else return "trend-stable";
    }

    public String getEngagementTrendClass() {
        if (avgEngagement == null) return "trend-stable";

        try {
            double engagement = Double.parseDouble(avgEngagement.replace("%", ""));
            if (engagement > 5.0) return "trend-up";
            else if (engagement < 1.0) return "trend-down";
            else return "trend-stable";
        } catch (Exception e) {
            return "trend-stable";
        }
    }
}