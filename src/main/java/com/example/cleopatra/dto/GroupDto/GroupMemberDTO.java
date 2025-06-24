package com.example.cleopatra.dto.GroupDto;
import com.example.cleopatra.enums.GroupRole;
import com.example.cleopatra.enums.MembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberDTO {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private String city;

    private GroupRole role;
    private MembershipStatus status;

    private LocalDateTime joinedAt;
    private LocalDateTime requestedAt;
    private LocalDateTime bannedAt;
    private String banReason;

    // Информация о том, кто забанил (если есть)
    private Long bannedByUserId;
    private String bannedByUserName;

    // Флаги для удобства фронтенда
    private Boolean canPromote;       // может ли повысить роль
    private Boolean canDemote;        // может ли понизить роль
    private Boolean canBan;           // может ли забанить
    private Boolean canUnban;         // может ли разбанить
    private Boolean canRemove;        // может ли удалить из группы
}

