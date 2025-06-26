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
public class MembershipActionResponse {

    private Boolean success;
    private String message;
    private GroupMembershipDto updatedMembership;
    private String action;
    private LocalDateTime timestamp;
}
