package com.example.cleopatra.dto.GroupDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PendingMembershipDto {

    private Long membershipId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private LocalDateTime requestedAt;

    // Дополнительная информация о пользователе
    private String bio;
    private Integer mutualFriendsCount;
    private Boolean isFollowedByCurrentUser;
}
