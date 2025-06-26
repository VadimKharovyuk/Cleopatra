package com.example.cleopatra.dto.GroupDto;

import com.example.cleopatra.enums.GroupRole;
import com.example.cleopatra.enums.MembershipStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


// DTO для отображения участника в настройках группы
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMembershipDto {

    private Long id;
    private Long userId;
    private Long groupId;

    // Информация о пользователе
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String userImageUrl;

    // Статус и роль
    private GroupRole role;
    private MembershipStatus status;

    // Даты
    private LocalDateTime requestedAt;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private LocalDateTime bannedAt;

    // Информация о бане
    private String banReason;
    private Long bannedByUserId;
    private String bannedByUserName;

    // Системные поля
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Вспомогательные флаги
    private Boolean canPromote;
    private Boolean canDemote;
    private Boolean canBan;
    private Boolean canUnban;
    private Boolean canRemove;
}