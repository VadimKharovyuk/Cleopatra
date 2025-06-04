package com.example.cleopatra.dto.ChatMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO для контактов пользователя
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContactDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String imageUrl;
    private Boolean isOnline;
    private String lastSeenText;
    private Boolean isFriend;
    private Boolean isFollowing;
    private Boolean isFollower;
    private Integer mutualFriendsCount;
    private LocalDateTime lastInteraction; // Последнее сообщение/взаимодействие
    private Boolean hasUnreadMessages;
    private Integer unreadCount;
}
