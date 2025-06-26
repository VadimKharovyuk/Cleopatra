package com.example.cleopatra.dto.GroupDto;

import com.example.cleopatra.enums.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для изменения роли участника
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChangeRoleRequest {

    private Long membershipId;
    private GroupRole newRole;
    private String reason;
}
