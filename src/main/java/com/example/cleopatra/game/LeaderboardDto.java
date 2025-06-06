package com.example.cleopatra.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDto {
    private String title;
    private List<PlayerStatsDto> players;
    private Integer currentUserRank;
    private Integer totalPlayers;
    private LocalDateTime generatedAt;

    public static LeaderboardDto empty(String title) {
        return LeaderboardDto.builder()
                .title(title)
                .players(List.of())
                .currentUserRank(null)
                .totalPlayers(0)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
