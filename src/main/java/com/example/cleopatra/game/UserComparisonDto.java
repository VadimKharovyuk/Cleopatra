package com.example.cleopatra.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserComparisonDto {
    private PlayerStatsDto currentUser;
    private PlayerStatsDto targetUser;
    private Map<String, Object> differences;
    private String comparisonSummary;

    public static UserComparisonDto create(PlayerStatsDto current, PlayerStatsDto target) {
        Map<String, Object> diff = Map.of(
                "scoreDifference", current.getBestScore() - target.getBestScore(),
                "questionDifference", current.getBestQuestionReached() - target.getBestQuestionReached(),
                "accuracyDifference", current.getAccuracyPercentage() - target.getAccuracyPercentage(),
                "gamesDifference", current.getTotalGamesPlayed() - target.getTotalGamesPlayed()
        );

        String summary = generateComparisonSummary(current, target);

        return UserComparisonDto.builder()
                .currentUser(current)
                .targetUser(target)
                .differences(diff)
                .comparisonSummary(summary)
                .build();
    }

    private static String generateComparisonSummary(PlayerStatsDto current, PlayerStatsDto target) {
        int scoreDiff = current.getBestScore() - target.getBestScore();

        if (scoreDiff > 0) {
            return String.format("Вы опережаете %s на %d очков", target.getUsername(), scoreDiff);
        } else if (scoreDiff < 0) {
            return String.format("%s опережает вас на %d очков", target.getUsername(), Math.abs(scoreDiff));
        } else {
            return String.format("У вас одинаковый результат с %s", target.getUsername());
        }
    }
}
