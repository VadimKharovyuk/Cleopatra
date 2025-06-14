package com.example.cleopatra.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsData {
    private long totalUsers;
    private long totalPosts;
    private long totalComments;
    private long totalStories;
    private long totalWallPosts;
    private long totalMessages;
    private long totalBlockedUsers;

    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
    private long activeUsersToday;
    private long onlineUsersNow;
    private long postsToday;
    private long messagesThisWeek;

    private double userGrowthPercentage;
    private double postGrowthPercentage;

    private Map<String, Long> usersRegistrationByMonth;
    private Map<String, Long> postsCreationByWeek;
}