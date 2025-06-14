package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.AdminAnalyticsData;
import lombok.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final UserService userService;
    private final CommentService commentService;
    private final PostService postService;
    private final StoryService storyService;
    private final WallPostService wallPostService;
    private final MessageService messageService;
    private final UserBlockService userBlockService;

    public AdminAnalyticsData getAnalyticsData() {
        return AdminAnalyticsData.builder()
                .totalUsers(getTotalUsers())
                .totalPosts(getTotalPosts())
                .totalComments(getTotalComments())
                .totalStories(getTotalStories())
                .totalWallPosts(getTotalWallPosts())
                .totalMessages(getTotalMessages())
                .totalBlockedUsers(getTotalBlockedUsers())
                .newUsersToday(getNewUsersToday())
                .newUsersThisWeek(getNewUsersThisWeek())
                .newUsersThisMonth(getNewUsersThisMonth())
                .activeUsersToday(getActiveUsersToday())
                .onlineUsersNow(getOnlineUsersNow())
                .postsToday(getPostsToday())
                .messagesThisWeek(getMessagesThisWeek())
                .userGrowthPercentage(getUserGrowthPercentage())
                .postGrowthPercentage(getPostGrowthPercentage())
                .usersRegistrationByMonth(getUsersRegistrationByMonth())
                .postsCreationByWeek(getPostsCreationByWeek())
                .build();
    }

    // Общие счетчики
    public long getTotalUsers() {
        return userService.getTotalUsersCount();
    }

    public long getTotalPosts() {
        return postService.getTotalPostsCount();
    }

    public long getTotalComments() {
        return commentService.getTotalCommentsCount();
    }

    public long getTotalStories() {
        return storyService.getTotalStoriesCount();
    }

    public long getTotalWallPosts() {
        return wallPostService.getTotalWallPostsCount();
    }

    public long getTotalMessages() {
        return messageService.getTotalMessagesCount();
    }

    public long getTotalBlockedUsers() {
        return userBlockService.getTotalBlockedUsersCount();
    }

    // Временные метрики
    public long getNewUsersToday() {
        LocalDate today = LocalDate.now();
        return userService.getUsersCountByDate(today);
    }

    public long getNewUsersThisWeek() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        return userService.getUsersCountFromDate(weekStart);
    }

    public long getNewUsersThisMonth() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        return userService.getUsersCountFromDate(monthStart);
    }

    // Активность пользователей
    public long getActiveUsersToday() {
        LocalDate today = LocalDate.now();
        return userService.getActiveUsersCountByDate(today);
    }

    public long getOnlineUsersNow() {
        return userService.getOnlineUsersCount();
    }

    public long getPostsToday() {
        LocalDate today = LocalDate.now();
        return postService.getPostsCountByDate(today);
    }

    public long getMessagesThisWeek() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        return messageService.getMessagesCountFromDate(weekStart);
    }

    // Расчет трендов
    public double getUserGrowthPercentage() {
        long thisWeekUsers = getNewUsersThisWeek();
        long lastWeekUsers = getNewUsersLastWeek();

        if (lastWeekUsers == 0) return thisWeekUsers > 0 ? 100.0 : 0.0;

        return Math.round(((double) (thisWeekUsers - lastWeekUsers) / lastWeekUsers) * 100 * 100.0) / 100.0;
    }

    public double getPostGrowthPercentage() {
        long thisWeekPosts = getPostsThisWeek();
        long lastWeekPosts = getPostsLastWeek();

        if (lastWeekPosts == 0) return thisWeekPosts > 0 ? 100.0 : 0.0;

        return Math.round(((double) (thisWeekPosts - lastWeekPosts) / lastWeekPosts) * 100 * 100.0) / 100.0;
    }

    private long getNewUsersLastWeek() {
        LocalDate lastWeekStart = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);
        return userService.getUsersCountBetweenDates(lastWeekStart, lastWeekEnd);
    }

    private long getPostsThisWeek() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        return postService.getPostsCountFromDate(weekStart);
    }

    private long getPostsLastWeek() {
        LocalDate lastWeekStart = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);
        return postService.getPostsCountBetweenDates(lastWeekStart, lastWeekEnd);
    }

    // Дополнительные аналитические методы
    public Map<String, Long> getUsersRegistrationByMonth() {
        Map<String, Long> result = new LinkedHashMap<>();

        for (int i = 11; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            String monthYear = date.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            long count = userService.getUsersCountByMonth(date.getYear(), date.getMonthValue());
            result.put(monthYear, count);
        }

        return result;
    }

    public Map<String, Long> getPostsCreationByWeek() {
        Map<String, Long> result = new LinkedHashMap<>();

        for (int i = 7; i >= 0; i--) {
            LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            String weekRange = weekStart.format(DateTimeFormatter.ofPattern("dd/MM")) +
                    " - " + weekEnd.format(DateTimeFormatter.ofPattern("dd/MM"));
            long count = postService.getPostsCountBetweenDates(weekStart, weekEnd);
            result.put(weekRange, count);
        }

        return result;
    }

    // Дополнительная аналитика по активности
    public Map<String, Long> getUserActivityStats() {
        Map<String, Long> stats = new LinkedHashMap<>();

        stats.put("Онлайн сейчас", getOnlineUsersNow());
        stats.put("Активны сегодня", getActiveUsersToday());
        stats.put("Активны за неделю", getActiveUsersThisWeek());
        stats.put("Новые сегодня", getNewUsersToday());

        return stats;
    }

    private long getActiveUsersThisWeek() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        return userService.getActiveUsersCountFromDate(weekStart);
    }




}