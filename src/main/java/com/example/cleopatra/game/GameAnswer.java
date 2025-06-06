package com.example.cleopatra.game;

import com.example.cleopatra.enums.DifficultyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @Column(name = "question_number")
    private Integer questionNumber;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "player_answer")
    private String playerAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "difficulty_level")
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    @Column(name = "points_earned")
    private Integer pointsEarned;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt = LocalDateTime.now();

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;
}