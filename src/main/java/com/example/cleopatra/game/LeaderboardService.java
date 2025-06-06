package com.example.cleopatra.game;


import com.example.cleopatra.enums.GameStatus;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final GameSessionRepository gameSessionRepository;
    private final GameAnswerRepository gameAnswerRepository;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    /**
     * Получить рейтинг среди подписок пользователя
     */
    public LeaderboardDto getSubscriptionsLeaderboard(User user, int limit) {
        // Получаем ID всех пользователей, на которых подписан user
        List<Long> subscriptionIds = subscriptionService.getSubscriptionIds(user.getId());

        // Добавляем самого пользователя для сравнения
        subscriptionIds.add(user.getId());

        if (subscriptionIds.isEmpty()) {
            return LeaderboardDto.builder()
                    .title("Рейтинг подписок")
                    .players(Collections.emptyList())
                    .currentUserRank(null)
                    .totalPlayers(0)
                    .build();
        }

        // Получаем лучшие игры этих пользователей
        List<GameSession> topGames = gameSessionRepository.findGamesByUserIds(subscriptionIds);

        // Группируем по пользователям и берем лучший результат каждого
        Map<Long, GameSession> bestGamesByUser = topGames.stream()
                .collect(Collectors.toMap(
                        game -> game.getPlayer().getId(),
                        game -> game,
                        (existing, replacement) -> {
                            // Сравниваем сначала по очкам, потом по достигнутому вопросу
                            if (existing.getTotalScore().equals(replacement.getTotalScore())) {
                                return existing.getCurrentQuestionNumber() >= replacement.getCurrentQuestionNumber()
                                        ? existing : replacement;
                            }
                            return existing.getTotalScore() >= replacement.getTotalScore() ? existing : replacement;
                        }
                ));

        List<PlayerStatsDto> playerStats = bestGamesByUser.values().stream()
                .map(this::convertToPlayerStats)
                .sorted(this::comparePlayerStats)
                .limit(limit)
                .collect(Collectors.toList());

        // Добавляем ранги и проверяем подписки
        for (int i = 0; i < playerStats.size(); i++) {
            PlayerStatsDto player = playerStats.get(i);
            player.setRank(i + 1);

            // Проверяем, подписан ли текущий пользователь на этого игрока
            if (!player.getUserId().equals(user.getId())) {
                player.setIsSubscribed(subscriptionService.isSubscribed(user.getId(), player.getUserId()));
            }
        }

        // Находим ранг текущего пользователя
        Integer currentUserRank = null;
        for (PlayerStatsDto stats : playerStats) {
            if (stats.getUserId().equals(user.getId())) {
                currentUserRank = stats.getRank();
                break;
            }
        }

        return LeaderboardDto.builder()
                .title("Рейтинг подписок")
                .players(playerStats)
                .currentUserRank(currentUserRank)
                .totalPlayers(subscriptionIds.size())
                .build();
    }

    /**
     * Получить глобальный рейтинг всех пользователей
     */
    public LeaderboardDto getGlobalLeaderboard(User currentUser, int limit) {
        List<GameSession> topGames = gameSessionRepository.findTopGamesByScore();

        // Группируем по пользователям и берем лучший результат каждого
        Map<Long, GameSession> bestGamesByUser = topGames.stream()
                .collect(Collectors.toMap(
                        game -> game.getPlayer().getId(),
                        game -> game,
                        (existing, replacement) -> {
                            if (existing.getTotalScore().equals(replacement.getTotalScore())) {
                                return existing.getCurrentQuestionNumber() >= replacement.getCurrentQuestionNumber()
                                        ? existing : replacement;
                            }
                            return existing.getTotalScore() >= replacement.getTotalScore() ? existing : replacement;
                        }
                ));

        List<PlayerStatsDto> playerStats = bestGamesByUser.values().stream()
                .map(this::convertToPlayerStats)
                .sorted(this::comparePlayerStats)
                .limit(limit)
                .collect(Collectors.toList());

        // Добавляем ранги и проверяем подписки
        for (int i = 0; i < playerStats.size(); i++) {
            PlayerStatsDto player = playerStats.get(i);
            player.setRank(i + 1);

            // Проверяем, подписан ли текущий пользователь на этого игрока
            if (!player.getUserId().equals(currentUser.getId())) {
                player.setIsSubscribed(subscriptionService.isSubscribed(currentUser.getId(), player.getUserId()));
            }
        }

        // Находим ранг текущего пользователя в глобальном рейтинге
        Integer currentUserRank = findUserGlobalRank(currentUser);

        return LeaderboardDto.builder()
                .title("Глобальный рейтинг")
                .players(playerStats)
                .currentUserRank(currentUserRank)
                .totalPlayers(bestGamesByUser.size())
                .build();
    }

    /**
     * Получить подробную статистику пользователя
     */
    public PlayerStatsDto getUserDetailedStats(User user) {
        // Основная статистика игр
        long totalGames = gameSessionRepository.countByPlayer(user);
        long completedGames = gameSessionRepository.countByPlayerAndGameStatus(user, GameStatus.COMPLETED);
        long failedGames = gameSessionRepository.countByPlayerAndGameStatus(user, GameStatus.FAILED);

        Optional<Integer> bestScore = gameSessionRepository.findMaxScoreByPlayer(user);
        Optional<Integer> bestQuestion = gameSessionRepository.findMaxQuestionReachedByPlayer(user);

        // Статистика ответов
        long totalAnswers = gameAnswerRepository.countTotalAnswersByPlayer(user.getId());
        long correctAnswers = gameAnswerRepository.countCorrectAnswersByPlayer(user.getId());
        Double accuracyPercentage = gameAnswerRepository.getAverageCorrectPercentageByPlayer(user.getId());

        // Последняя игра
        List<GameSession> recentGames = gameSessionRepository.findByPlayerOrderByStartedAtDesc(user);
        GameSession lastGame = recentGames.isEmpty() ? null : recentGames.get(0);

        return PlayerStatsDto.builder()
                .userId(user.getId())
                .username(getUserDisplayName(user))
                .avatarUrl(user.getImageUrl())
                .bestScore(bestScore.orElse(0))
                .bestQuestionReached(bestQuestion.orElse(0))
                .totalGamesPlayed((int) totalGames)
                .gamesCompleted((int) completedGames)
                .gamesFailed((int) failedGames)
                .totalAnswers((int) totalAnswers)
                .correctAnswers((int) correctAnswers)
                .accuracyPercentage(accuracyPercentage != null ? accuracyPercentage.floatValue() * 100 : 0f)
                .lastPlayed(lastGame != null ? lastGame.getStartedAt() : null)
                .isSubscribed(false)
                .build();
    }

    /**
     * Получить сравнение с конкретным пользователем
     */
    public Map<String, Object> compareWithUser(User currentUser, User targetUser) {
        PlayerStatsDto currentStats = getUserDetailedStats(currentUser);
        PlayerStatsDto targetStats = getUserDetailedStats(targetUser);

        // Проверяем подписку
        targetStats.setIsSubscribed(subscriptionService.isSubscribed(currentUser.getId(), targetUser.getId()));

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("currentUser", currentStats);
        comparison.put("targetUser", targetStats);

        // Статистика сравнения
        Map<String, Object> diff = new HashMap<>();
        diff.put("scoreDifference", currentStats.getBestScore() - targetStats.getBestScore());
        diff.put("questionDifference", currentStats.getBestQuestionReached() - targetStats.getBestQuestionReached());
        diff.put("accuracyDifference", currentStats.getAccuracyPercentage() - targetStats.getAccuracyPercentage());
        diff.put("gamesDifference", currentStats.getTotalGamesPlayed() - targetStats.getTotalGamesPlayed());

        comparison.put("differences", diff);

        return comparison;
    }

    /**
     * Получить позицию пользователя в рейтинге подписок
     */
    public Integer getUserRankAmongSubscriptions(User user) {
        List<Long> subscriptionIds = subscriptionService.getSubscriptionIds(user.getId());
        subscriptionIds.add(user.getId());

        List<GameSession> topGames = gameSessionRepository.findGamesByUserIds(subscriptionIds);

        Map<Long, GameSession> bestGamesByUser = topGames.stream()
                .collect(Collectors.toMap(
                        game -> game.getPlayer().getId(),
                        game -> game,
                        (existing, replacement) -> {
                            if (existing.getTotalScore().equals(replacement.getTotalScore())) {
                                return existing.getCurrentQuestionNumber() >= replacement.getCurrentQuestionNumber()
                                        ? existing : replacement;
                            }
                            return existing.getTotalScore() >= replacement.getTotalScore() ? existing : replacement;
                        }
                ));

        List<PlayerStatsDto> sortedStats = bestGamesByUser.values().stream()
                .map(this::convertToPlayerStats)
                .sorted(this::comparePlayerStats)
                .collect(Collectors.toList());

        for (int i = 0; i < sortedStats.size(); i++) {
            if (sortedStats.get(i).getUserId().equals(user.getId())) {
                return i + 1;
            }
        }

        return null;
    }

    // ============ Приватные методы ============

    private PlayerStatsDto convertToPlayerStats(GameSession gameSession) {
        User player = gameSession.getPlayer();

        // Получаем дополнительную статистику
        long totalGames = gameSessionRepository.countByPlayer(player);
        long completedGames = gameSessionRepository.countByPlayerAndGameStatus(player, GameStatus.COMPLETED);

        return PlayerStatsDto.builder()
                .userId(player.getId())
                .username(getUserDisplayName(player))
                .avatarUrl(player.getImageUrl())
                .bestScore(gameSession.getTotalScore())
                .bestQuestionReached(gameSession.getCurrentQuestionNumber())
                .totalGamesPlayed((int) totalGames)
                .gamesCompleted((int) completedGames)
                .lastPlayed(gameSession.getStartedAt())
                .build();
    }

    private int comparePlayerStats(PlayerStatsDto p1, PlayerStatsDto p2) {
        // Сортируем по очкам, потом по достигнутому вопросу, потом по дате
        int scoreCompare = Integer.compare(p2.getBestScore(), p1.getBestScore());
        if (scoreCompare != 0) return scoreCompare;

        int questionCompare = Integer.compare(p2.getBestQuestionReached(), p1.getBestQuestionReached());
        if (questionCompare != 0) return questionCompare;

        return p2.getLastPlayed().compareTo(p1.getLastPlayed());
    }

    private Integer findUserGlobalRank(User user) {
        List<GameSession> allTopGames = gameSessionRepository.findTopGamesByScore();

        Map<Long, GameSession> bestGamesByUser = allTopGames.stream()
                .collect(Collectors.toMap(
                        game -> game.getPlayer().getId(),
                        game -> game,
                        (existing, replacement) -> {
                            if (existing.getTotalScore().equals(replacement.getTotalScore())) {
                                return existing.getCurrentQuestionNumber() >= replacement.getCurrentQuestionNumber()
                                        ? existing : replacement;
                            }
                            return existing.getTotalScore() >= replacement.getTotalScore() ? existing : replacement;
                        }
                ));

        List<PlayerStatsDto> sortedStats = bestGamesByUser.values().stream()
                .map(this::convertToPlayerStats)
                .sorted(this::comparePlayerStats)
                .collect(Collectors.toList());

        for (int i = 0; i < sortedStats.size(); i++) {
            if (sortedStats.get(i).getUserId().equals(user.getId())) {
                return i + 1;
            }
        }

        return null;
    }

    private String getUserDisplayName(User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";

        if (firstName.isEmpty() && lastName.isEmpty()) {
            return user.getEmail().split("@")[0]; // fallback to email username
        }

        return (firstName + " " + lastName).trim();
    }
}
