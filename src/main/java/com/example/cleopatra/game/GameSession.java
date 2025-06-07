package com.example.cleopatra.game;

import com.example.cleopatra.enums.GameStatus;
import com.example.cleopatra.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(name = "current_question_number")
    private Integer currentQuestionNumber = 1;

    @Column(name = "total_score")
    private Integer totalScore = 0;

    @Column(name = "game_status")
    @Enumerated(EnumType.STRING)
    private GameStatus gameStatus = GameStatus.IN_PROGRESS;

    @Column(name = "fifty_fifty_used")
    private Boolean fiftyFiftyUsed = false;

    @Column(name = "skip_question_used")
    private Boolean skipQuestionUsed = false;

    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GameAnswer> answers = new ArrayList<>();

    @Column(name = "current_question_text", columnDefinition = "TEXT")
    private String currentQuestionText;

    @Column(name = "current_correct_answer")
    private String currentCorrectAnswer;

    @Column(name = "current_question_data", columnDefinition = "TEXT")
    private String currentQuestionData;
}
