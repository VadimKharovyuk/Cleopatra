package com.example.cleopatra.dto.GroupDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для бана участника
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BanMemberRequest {

    private Long membershipId;
    private String reason;
    private Boolean permanent; // true для постоянного бана
}
