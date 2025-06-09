package com.example.cleopatra.dto.BlockedUse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBlockResponse {
    private Long id;
    private Long blockerId;
    private Long blockedId;
    private LocalDateTime blockedAt;
}
