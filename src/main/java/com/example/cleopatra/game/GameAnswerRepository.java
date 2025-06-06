package com.example.cleopatra.game;

import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameAnswerRepository extends JpaRepository<GameAnswer, Long> {

    // Найти все ответы для игровой сессии
    List<GameAnswer> findByGameSessionOrderByQuestionNumber(GameSession gameSession);

    // Статистика ответов пользователя
    @Query("SELECT COUNT(ga) FROM GameAnswer ga WHERE ga.gameSession.player.id = :playerId AND ga.isCorrect = true")
    long countCorrectAnswersByPlayer(@Param("playerId") Long playerId);

    @Query("SELECT COUNT(ga) FROM GameAnswer ga WHERE ga.gameSession.player.id = :playerId")
    long countTotalAnswersByPlayer(@Param("playerId") Long playerId);

    // Средний процент правильных ответов
    @Query("SELECT AVG(CASE WHEN ga.isCorrect = true THEN 1.0 ELSE 0.0 END) FROM GameAnswer ga " +
            "WHERE ga.gameSession.player.id = :playerId")
    Double getAverageCorrectPercentageByPlayer(@Param("playerId") Long playerId);

    // Статистика по категориям
    @Query("SELECT ga.difficultyLevel, COUNT(ga), AVG(CASE WHEN ga.isCorrect = true THEN 1.0 ELSE 0.0 END) " +
            "FROM GameAnswer ga WHERE ga.gameSession.player.id = :playerId " +
            "GROUP BY ga.difficultyLevel")
    List<Object[]> getDifficultyStatsForPlayer(@Param("playerId") Long playerId);
}