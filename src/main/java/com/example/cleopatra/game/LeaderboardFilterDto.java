package com.example.cleopatra.game;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardFilterDto {
    private String period; // "today", "week", "month", "all"
    private String type;   // "subscriptions", "global", "friends"
    private Integer limit; // количество пользователей в рейтинге
    private String sortBy; // "score", "questions", "accuracy", "games"

    public static LeaderboardFilterDto getDefault() {
        return LeaderboardFilterDto.builder()
                .period("all")
                .type("subscriptions")
                .limit(50)
                .sortBy("score")
                .build();
    }
}
