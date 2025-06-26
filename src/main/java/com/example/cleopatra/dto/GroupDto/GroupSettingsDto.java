package com.example.cleopatra.dto.GroupDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO для настроек группы
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupSettingsDto {

    private Long groupId;
    private String groupName;
    private String groupDescription;

    // Статистика
    private Long totalMembers;
    private Long pendingRequests;
    private Long bannedMembersCount; // Переименовано с bannedMembers

    // Списки участников
    private List<GroupMembershipDto> members;
    private List<PendingMembershipDto> pendingMemberships;
    private List<GroupMembershipDto> bannedMembersList; // Переименовано с bannedMembers

    // Права текущего пользователя
    private Boolean canManageMembers;
    private Boolean canBanMembers;
    private Boolean canChangeRoles;
    private Boolean canDeleteGroup;
}