package com.example.cleopatra.dto.GroupDto;

import com.example.cleopatra.enums.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO для краткой информации об участнике (для списков)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberSummaryDto {

    private Long membershipId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String imageUrl;
    private GroupRole role;
    private LocalDateTime joinedAt;
    private Boolean isOnline; // статус онлайн (если реализовано)
    private LocalDateTime lastSeen;
}
