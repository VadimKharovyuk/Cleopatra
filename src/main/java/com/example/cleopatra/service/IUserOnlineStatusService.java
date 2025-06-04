package com.example.cleopatra.service;

import com.example.cleopatra.model.UserOnlineStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface IUserOnlineStatusService {
    void updateOnlineStatus(Long userId, boolean isOnline);
    void updateOnlineStatus(Long userId, boolean isOnline, String clientInfo);
    void updateLastSeen(Long userId);
    boolean isUserOnline(Long userId);
    LocalDateTime getLastSeen(Long userId);
    boolean wasRecentlyOnline(Long userId, int minutesAgo);
    List<UserOnlineStatus> getOnlineUsers();
    Long getOnlineUsersCount();
    int markInactiveUsersOffline(int minutesInactive);
    List<UserOnlineStatus> getUsersOnlineStatus(List<Long> userIds);
}
