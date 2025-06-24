package com.example.cleopatra.dto.GroupDto;


import com.example.cleopatra.enums.GroupStatus;
import com.example.cleopatra.enums.GroupType;
import com.example.cleopatra.enums.MembershipStatus;
import com.example.cleopatra.enums.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String imgId;
    private String backgroundImageUrl;
    private String backgroundImgId;

    private GroupType groupType;
    private GroupStatus groupStatus;
    private Long memberCount;

    // Информация о владельце (минимальная)
    private Long ownerId;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerImageUrl;

    // Статус текущего пользователя в группе (если авторизован)
    private MembershipStatus currentUserMembershipStatus;
    private GroupRole currentUserRole;
    private LocalDateTime currentUserJoinedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Дополнительные флаги для удобства фронтенда
    private Boolean canJoin;          // может ли текущий юзер присоединиться
    private Boolean canLeave;         // может ли покинуть
    private Boolean canManage;        // может ли управлять группой
    private Boolean isMember;         // является ли участником
}