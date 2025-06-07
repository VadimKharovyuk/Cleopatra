package com.example.cleopatra.game;

import com.example.cleopatra.dto.*;
import com.example.cleopatra.game.*;
import com.example.cleopatra.model.User;

import com.example.cleopatra.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Game", description = "API для игры 'Кто хочет стать миллионером'")
public class GameController {

    private final GameService gameService;
    private final UserService userService;
    private final LeaderboardService leaderboardService;
    private final GameSessionRepository gameSessionRepository;

    @PostMapping("/start")
    @Operation(summary = "Начать новую игру", description = "Создает новую игровую сессию для пользователя")
    @ApiResponse(responseCode = "200", description = "Игра успешно начата")
    @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    @ApiResponse(responseCode = "500", description = "Ошибка при получении вопроса")
    public ResponseEntity<GameSessionResponse> startNewGame(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            log.info("Пользователь {} начинает новую игру", user.getEmail());

            GameSessionResponse response = gameService.startNewGame(user);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при начале новой игры", e);
            return ResponseEntity.internalServerError()
                    .body(GameSessionResponse.builder()
                            .gameOver(true)
                            .build());
        }
    }

    @PostMapping("/answer")
    @ResponseBody
    public ResponseEntity<GameSessionResponse> answerQuestion(
            @RequestBody AnswerRequest request,
            Authentication authentication) {

        try {
            log.info("Received answer request: sessionId={}, playerAnswer='{}'",
                    request.getSessionId(), request.getPlayerAnswer());

            if (authentication == null) {
                log.error("No authentication provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userService.getCurrentUserEntity(authentication);
            log.info("Processing answer for user: {}", user.getEmail());

            // Валидация запроса
            if (request.getSessionId() == null) {
                log.error("Session ID is null");
                return ResponseEntity.badRequest()
                        .body(GameSessionResponse.builder()
                                .gameOver(true)
                                .build());
            }

            if (request.getPlayerAnswer() == null || request.getPlayerAnswer().trim().isEmpty()) {
                log.error("Player answer is null or empty");
                return ResponseEntity.badRequest()
                        .body(GameSessionResponse.builder()
                                .sessionId(request.getSessionId())
                                .gameOver(true)
                                .build());
            }

            GameSessionResponse response = gameService.answerQuestion(
                    request.getSessionId(),
                    request.getPlayerAnswer().trim(),
                    user.getId()
            );

            log.info("Answer processed successfully for session {}", request.getSessionId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Runtime error processing answer: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(GameSessionResponse.builder()
                            .sessionId(request.getSessionId())
                            .gameOver(true)
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error processing answer", e);
            return ResponseEntity.internalServerError()
                    .body(GameSessionResponse.builder()
                            .sessionId(request.getSessionId())
                            .gameOver(true)
                            .build());
        }
    }

    @PostMapping("/hint")
    @Operation(summary = "Использовать подсказку", description = "Применить одну из доступных подсказок")
    @ApiResponse(responseCode = "200", description = "Подсказка применена")
    @ApiResponse(responseCode = "400", description = "Подсказка недоступна или уже использована")
    public ResponseEntity<GameSessionResponse> useHint(
            @Valid @RequestBody HintRequest request,
            Authentication authentication) {

        try {
            User user = getCurrentUser(authentication);
            log.info("Пользователь {} использует подсказку {} в сессии {}",
                    user.getEmail(), request.getHintType(), request.getSessionId());

            GameSessionResponse response = gameService.useHint(
                    request.getSessionId(),
                    request.getHintType(),
                    user.getId()
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Ошибка при использовании подсказки: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(GameSessionResponse.builder()
                            .sessionId(request.getSessionId())
                            .build());
        } catch (Exception e) {
            log.error("Неожиданная ошибка при использовании подсказки", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @PostMapping("/{sessionId}/abandon")
    @Operation(summary = "Сдаться", description = "Завершить игру досрочно")
    @ApiResponse(responseCode = "200", description = "Игра завершена")
    public ResponseEntity<GameSessionResponse> abandonGame(
            @Parameter(description = "ID игровой сессии") @PathVariable Long sessionId,
            Authentication authentication) {

        try {
            User user = getCurrentUser(authentication);
            log.info("Пользователь {} сдается в игре {}", user.getEmail(), sessionId);

            GameSessionResponse response = gameService.abandonGame(sessionId, user.getId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Ошибка при завершении игры: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Неожиданная ошибка при завершении игры", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{sessionId}/status")
    @Operation(summary = "Получить статус игры", description = "Проверить текущее состояние игровой сессии")
    @ApiResponse(responseCode = "200", description = "Статус получен")
    @ApiResponse(responseCode = "404", description = "Сессия не найдена")
    public ResponseEntity<GameStateResponse> getGameStatus(
            @Parameter(description = "ID игровой сессии") @PathVariable Long sessionId,
            Authentication authentication) {

        try {
            User user = getCurrentUser(authentication);

            Optional<GameStateResponse> gameState = gameService.getGameStatus(sessionId, user);

            if (gameState.isPresent()) {
                return ResponseEntity.ok(gameState.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Ошибка при получении статуса игры", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/current")
    @Operation(summary = "Получить текущую игру", description = "Найти активную игровую сессию пользователя")
    @ApiResponse(responseCode = "200", description = "Текущая игра найдена")
    @ApiResponse(responseCode = "404", description = "Активная игра не найдена")
    public ResponseEntity<GameSessionResponse> getCurrentGame(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);

            Optional<GameSessionResponse> currentGame = gameService.getCurrentGame(user);

            if (currentGame.isPresent()) {
                return ResponseEntity.ok(currentGame.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Ошибка при поиске текущей игры", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/history")
    @Operation(summary = "История игр", description = "Получить историю игр пользователя")
    @ApiResponse(responseCode = "200", description = "История получена")
    public ResponseEntity<GameHistoryPageDto> getGameHistory(
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            User user = getCurrentUser(authentication);

            Pageable pageable = PageRequest.of(page, size);
            Page<GameHistoryDto> historyPage = gameService.getGameHistory(user, pageable);

            // Получаем общую статистику
            PlayerStatsDto stats = leaderboardService.getUserDetailedStats(user);

            GameHistoryPageDto response = GameHistoryPageDto.builder()
                    .games(historyPage.getContent())
                    .totalElements(historyPage.getTotalElements())
                    .currentPage(historyPage.getNumber())
                    .pageSize(historyPage.getSize())
                    .totalPages(historyPage.getTotalPages())
                    .hasNext(historyPage.hasNext())
                    .hasPrevious(historyPage.hasPrevious())
                    .summary(convertToPlayerGameStats(stats))
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при получении истории игр", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика игрока", description = "Получить общую статистику игрока")
    @ApiResponse(responseCode = "200", description = "Статистика получена")
    public ResponseEntity<PlayerStatsDto> getPlayerStats(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            PlayerStatsDto stats = leaderboardService.getUserDetailedStats(user);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Ошибка при получении статистики игрока", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{sessionId}/details")
    @Operation(summary = "Детали игры", description = "Получить подробную информацию об игре")
    @ApiResponse(responseCode = "200", description = "Детали получены")
    @ApiResponse(responseCode = "404", description = "Игра не найдена")
    public ResponseEntity<GameDetailDto> getGameDetails(
            @Parameter(description = "ID игровой сессии") @PathVariable Long sessionId,
            Authentication authentication) {

        try {
            User user = getCurrentUser(authentication);
            GameDetailDto details = gameService.getGameDetail(sessionId, user);
            return ResponseEntity.ok(details);

        } catch (RuntimeException e) {
            log.error("Игра не найдена: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Ошибка при получении деталей игры", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/can-resume")
    @Operation(summary = "Проверить возможность продолжения", description = "Проверить есть ли активная игра для продолжения")
    @ApiResponse(responseCode = "200", description = "Проверка выполнена")
    public ResponseEntity<Boolean> canResumeGame(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            boolean canResume = gameService.canResumeGame(user);
            return ResponseEntity.ok(canResume);

        } catch (Exception e) {
            log.error("Ошибка при проверке возможности продолжения игры", e);
            return ResponseEntity.ok(false);
        }
    }

    // ============ Вспомогательные методы ============

    private User getCurrentUser(Authentication authentication) {
       return userService.getCurrentUserEntity(authentication);
    }

    private PlayerGameStatsDto convertToPlayerGameStats(PlayerStatsDto stats) {
        return PlayerGameStatsDto.builder()
                .totalGames(stats.getTotalGamesPlayed())
                .completedGames(stats.getGamesCompleted())
                .failedGames(stats.getGamesFailed())
                .bestScore(stats.getBestScore())
                .maxQuestionReached(stats.getBestQuestionReached())
                .totalAnswers(stats.getTotalAnswers())
                .correctAnswers(stats.getCorrectAnswers())
                .accuracyPercentage(stats.getAccuracyPercentage())
                .lastPlayedAt(stats.getLastPlayed())
                .build();
    }




    // Добавьте этот метод в GameController

    @GetMapping("/debug/session/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugSession(
            @PathVariable Long sessionId,
            Authentication authentication) {

        try {
            User user = userService.getCurrentUserEntity(authentication);

            Optional<GameSession> sessionOpt = gameSessionRepository.findById(sessionId);

            Map<String, Object> debug = new HashMap<>();
            debug.put("sessionExists", sessionOpt.isPresent());
            debug.put("requestingUserId", user.getId());
            debug.put("requestingUserEmail", user.getEmail());

            if (sessionOpt.isPresent()) {
                GameSession session = sessionOpt.get();
                debug.put("sessionStatus", session.getGameStatus());
                debug.put("sessionOwnerId", session.getPlayer().getId());
                debug.put("sessionOwnerEmail", session.getPlayer().getEmail());
                debug.put("currentQuestion", session.getCurrentQuestionNumber());
                debug.put("totalScore", session.getTotalScore());
                debug.put("startedAt", session.getStartedAt());
                debug.put("finishedAt", session.getFinishedAt());
                debug.put("isOwner", session.getPlayer().getId().equals(user.getId()));
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ============ Exception Handlers ============

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GameSessionResponse> handleRuntimeException(RuntimeException e) {
        log.error("Ошибка выполнения: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(GameSessionResponse.builder()
                        .gameOver(true)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GameSessionResponse> handleException(Exception e) {
        log.error("Неожиданная ошибка", e);
        return ResponseEntity.internalServerError()
                .body(GameSessionResponse.builder()
                        .gameOver(true)
                        .build());
    }
}