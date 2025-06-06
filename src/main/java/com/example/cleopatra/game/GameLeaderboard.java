package com.example.cleopatra.game;

import com.example.cleopatra.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_leaderboard")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameLeaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "best_score")
    private Integer bestScore = 0;

    @Column(name = "total_games_played")
    private Integer totalGamesPlayed = 0;

    @Column(name = "games_completed")
    private Integer gamesCompleted = 0;

    @Column(name = "best_question_reached")
    private Integer bestQuestionReached = 0;

    @Column(name = "last_played")
    private LocalDateTime lastPlayed;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
