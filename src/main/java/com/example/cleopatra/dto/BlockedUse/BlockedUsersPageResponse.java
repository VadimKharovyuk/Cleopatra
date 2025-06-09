package com.example.cleopatra.dto.BlockedUse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockedUsersPageResponse {
    private List<BlockedUserDto> blockedUsers;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    private long blockedUsersCount; // Общее количество заблокированных
}
