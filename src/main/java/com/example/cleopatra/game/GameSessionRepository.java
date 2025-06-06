package com.example.cleopatra.game;

import com.example.cleopatra.enums.GameStatus;
import com.example.cleopatra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    // Найти активную игру пользователя
    Optional<GameSession> findByPlayerAndGameStatus(User player, GameStatus gameStatus);

    // Найти игру по ID, пользователю и статусу
    Optional<GameSession> findByIdAndPlayerIdAndGameStatus(Long id, Long playerId, GameStatus gameStatus);

    // Найти игру по ID и пользователю (любой статус)
    Optional<GameSession> findByIdAndPlayerId(Long id, Long playerId);

    // Найти все игры пользователя
    List<GameSession> findByPlayerOrderByStartedAtDesc(User player);

    // Найти все игры пользователя с пагинацией
    Page<GameSession> findByPlayerOrderByStartedAtDesc(User player, Pageable pageable);

    // Статистика пользователя
    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.player = :player")
    long countByPlayer(@Param("player") User player);

    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.player = :player AND gs.gameStatus = :status")
    long countByPlayerAndGameStatus(@Param("player") User player, @Param("status") GameStatus status);

    @Query("SELECT MAX(gs.totalScore) FROM GameSession gs WHERE gs.player = :player")
    Optional<Integer> findMaxScoreByPlayer(@Param("player") User player);

    @Query("SELECT MAX(gs.currentQuestionNumber) FROM GameSession gs WHERE gs.player = :player")
    Optional<Integer> findMaxQuestionReachedByPlayer(@Param("player") User player);

    // Топ игроков (лучшие результаты)
    @Query("SELECT gs FROM GameSession gs WHERE gs.gameStatus IN ('COMPLETED', 'FAILED') " +
            "ORDER BY gs.totalScore DESC, gs.currentQuestionNumber DESC")
    List<GameSession> findTopGamesByScore();

    // Найти игры друзей/подписок для рейтинга
    @Query("SELECT gs FROM GameSession gs " +
            "WHERE gs.player.id IN :userIds " +
            "AND gs.gameStatus IN ('COMPLETED', 'FAILED') " +
            "ORDER BY gs.totalScore DESC, gs.finishedAt DESC")
    List<GameSession> findGamesByUserIds(@Param("userIds") List<Long> userIds);
}