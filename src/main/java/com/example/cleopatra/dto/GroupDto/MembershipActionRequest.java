package com.example.cleopatra.dto.GroupDto;

import com.example.cleopatra.enums.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для ответа на заявку
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MembershipActionRequest {

    private Long membershipId;
    private String action; // "approve", "reject", "ban", "promote", "demote", "remove"
    private GroupRole newRole; // для promote/demote
    private String reason; // для ban/reject
}