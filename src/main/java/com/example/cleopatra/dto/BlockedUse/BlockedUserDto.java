package com.example.cleopatra.dto.BlockedUse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockedUserDto {
    // Информация о блокировке
    private Long blockId;
    private LocalDateTime blockedAt;

    // Информация о заблокированном пользователе
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private String city;
    private Boolean isOnline;
    private LocalDateTime lastSeen;
    private Long followersCount;
    private Long followingCount;
}
