package com.example.cleopatra.game;

import com.example.cleopatra.enums.DifficultyLevel;
import com.example.cleopatra.enums.GameStatus;
import com.example.cleopatra.enums.HintType;

import com.example.cleopatra.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GameService {

    private final QuestionProvider questionProvider;
    private final GameSessionRepository gameSessionRepository;
    private final GameAnswerRepository gameAnswerRepository;

    private static final int MAX_QUESTIONS = 100;
    private static final int TIME_LIMIT_SECONDS = 30;

    /**
     * Начать новую игру
     */
    private final Map<Long, Question> currentQuestionCache = new ConcurrentHashMap<>();

    // Измените метод startNewGame:
    public GameSessionResponse startNewGame(User user) {
        // Завершаем активную игру если есть
        finishActiveGame(user);

        GameSession gameSession = GameSession.builder()
                .player(user)
                .currentQuestionNumber(1)
                .totalScore(0)
                .gameStatus(GameStatus.IN_PROGRESS)
                .fiftyFiftyUsed(false)
                .skipQuestionUsed(false)
                .startedAt(LocalDateTime.now())
                .build();

        gameSession = gameSessionRepository.save(gameSession);

        // Получаем первый вопрос
        Question question = getQuestionForLevel(1);
        if (question == null) {
            throw new RuntimeException("Не удалось получить вопрос");
        }

        // КЭШИРУЕМ ТЕКУЩИЙ ВОПРОС
        currentQuestionCache.put(gameSession.getId(), question);
        log.info("Cached question for session {}: {}", gameSession.getId(), question.getQuestionText());

        return GameSessionResponse.builder()
                .sessionId(gameSession.getId())
                .currentQuestion(1)
                .totalScore(0)
                .question(question)
                .availableHints(getAvailableHints(gameSession))
                .timeLimit(TIME_LIMIT_SECONDS)
                .build();
    }


    /**
     * Использовать подсказку
     */
    public GameSessionResponse useHint(Long sessionId, HintType hintType, Long userId) {
        GameSession session = getActiveGameSession(sessionId, userId);

        // Проверяем доступность подсказки
        if (!isHintAvailable(session, hintType)) {
            throw new RuntimeException("Подсказка недоступна");
        }

        Question currentQuestion = getQuestionForLevel(session.getCurrentQuestionNumber());
        if (currentQuestion == null) {
            throw new RuntimeException("Не удалось получить текущий вопрос");
        }

        switch (hintType) {
            case FIFTY_FIFTY:
                session.setFiftyFiftyUsed(true);
                currentQuestion.applyFiftyFifty();
                break;

            case SKIP_QUESTION:
                session.setSkipQuestionUsed(true);
                return skipCurrentQuestion(session);
        }

        gameSessionRepository.save(session);

        return GameSessionResponse.builder()
                .sessionId(sessionId)
                .currentQuestion(session.getCurrentQuestionNumber())
                .totalScore(session.getTotalScore())
                .question(currentQuestion)
                .availableHints(getAvailableHints(session))
                .timeLimit(TIME_LIMIT_SECONDS)
                .hintUsed(hintType)
                .build();
    }

    /**
     * Сдаться и завершить игру
     */
    public GameSessionResponse abandonGame(Long sessionId, Long userId) {
        GameSession session = getActiveGameSession(sessionId, userId);

        session.setGameStatus(GameStatus.ABANDONED);
        session.setFinishedAt(LocalDateTime.now());
        gameSessionRepository.save(session);

        return GameSessionResponse.builder()
                .sessionId(sessionId)
                .gameOver(true)
                .finalScore(session.getTotalScore())
                .questionsAnswered(session.getCurrentQuestionNumber() - 1)
                .build();
    }

    // ============ Приватные методы ============

    private GameSession getActiveGameSession(Long sessionId, Long userId) {
        return gameSessionRepository.findByIdAndPlayerIdAndGameStatus(
                        sessionId, userId, GameStatus.IN_PROGRESS)
                .orElseThrow(() -> new RuntimeException("Активная игровая сессия не найдена"));
    }

    private void finishActiveGame(User user) {
        gameSessionRepository.findByPlayerAndGameStatus(user, GameStatus.IN_PROGRESS)
                .ifPresent(session -> {
                    session.setGameStatus(GameStatus.ABANDONED);
                    session.setFinishedAt(LocalDateTime.now());
                    gameSessionRepository.save(session);
                });
    }

    private Question getQuestionForLevel(int questionNumber) {
        DifficultyLevel difficulty = getDifficultyForQuestion(questionNumber);
        return questionProvider.getQuestion(difficulty);
    }

    private DifficultyLevel getDifficultyForQuestion(int questionNumber) {
        for (DifficultyLevel level : DifficultyLevel.values()) {
            if (questionNumber >= level.getMinQuestion() && questionNumber <= level.getMaxQuestion()) {
                return level;
            }
        }
        return DifficultyLevel.GENIUS; // По умолчанию самый сложный
    }

    private void saveGameAnswer(GameSession session, Question question, String playerAnswer,
                                boolean isCorrect, int pointsEarned) {
        GameAnswer gameAnswer = GameAnswer.builder()
                .gameSession(session)
                .questionNumber(session.getCurrentQuestionNumber())
                .questionText(question.getQuestionText())
                .correctAnswer(question.getCorrectAnswer())
                .playerAnswer(playerAnswer)
                .isCorrect(isCorrect)
                .difficultyLevel(question.getDifficulty())
                .pointsEarned(pointsEarned)
                .answeredAt(LocalDateTime.now())
                .timeTakenSeconds(calculateTimeTaken(session))
                .build();

        gameAnswerRepository.save(gameAnswer);
    }

    private int calculateTimeTaken(GameSession session) {
        // Упрощенный расчет - в реальности нужно отслеживать время начала вопроса
        return (int) ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
    }


    private boolean isTimeExpired(GameSession session) {
        LocalDateTime now = LocalDateTime.now();
        long secondsSinceStart = ChronoUnit.SECONDS.between(session.getStartedAt(), now);
        long expectedTime = (long) (session.getCurrentQuestionNumber() - 1) * TIME_LIMIT_SECONDS;
        long timeOnCurrentQuestion = secondsSinceStart - expectedTime;

        // Даем 60 секунд на каждый вопрос вместо 30 - этого достаточно для комфортной игры
        boolean expired = timeOnCurrentQuestion > 60;

        // ПОДРОБНОЕ ЛОГИРОВАНИЕ ДЛЯ ОТЛАДКИ
        log.info("=== TIME CHECK DEBUG ===");
        log.info("Session ID: {}", session.getId());
        log.info("Session started at: {}", session.getStartedAt());
        log.info("Current time: {}", now);
        log.info("Seconds since start: {}", secondsSinceStart);
        log.info("Current question number: {}", session.getCurrentQuestionNumber());
        log.info("Expected time for previous questions: {}", expectedTime);
        log.info("Time on current question: {}", timeOnCurrentQuestion);
        log.info("Time limit: 60 seconds");
        log.info("Is expired: {}", expired);
        log.info("========================");

        return expired;
    }

    private GameSessionResponse handleTimeExpired(GameSession session) {
        session.setGameStatus(GameStatus.FAILED);
        session.setFinishedAt(LocalDateTime.now());
        gameSessionRepository.save(session);

        return GameSessionResponse.builder()
                .sessionId(session.getId())
                .gameOver(true)
                .timeExpired(true)
                .finalScore(session.getTotalScore())
                .questionsAnswered(session.getCurrentQuestionNumber() - 1)
                .build();
    }

    private GameSessionResponse skipCurrentQuestion(GameSession session) {
        // Переходим к следующему вопросу без сохранения ответа
        session.setCurrentQuestionNumber(session.getCurrentQuestionNumber() + 1);
        gameSessionRepository.save(session);

        if (session.getCurrentQuestionNumber() > MAX_QUESTIONS) {
            session.setGameStatus(GameStatus.COMPLETED);
            session.setFinishedAt(LocalDateTime.now());
            gameSessionRepository.save(session);

            return GameSessionResponse.builder()
                    .sessionId(session.getId())
                    .gameOver(true)
                    .gameCompleted(true)
                    .finalScore(session.getTotalScore())
                    .questionsAnswered(session.getCurrentQuestionNumber() - 1)
                    .build();
        }

        Question nextQuestion = getQuestionForLevel(session.getCurrentQuestionNumber());

        return GameSessionResponse.builder()
                .sessionId(session.getId())
                .currentQuestion(session.getCurrentQuestionNumber())
                .totalScore(session.getTotalScore())
                .question(nextQuestion)
                .availableHints(getAvailableHints(session))
                .timeLimit(TIME_LIMIT_SECONDS)
                .questionSkipped(true)
                .build();
    }

    private String[] getAvailableHints(GameSession session) {
        return new String[]{
                !session.getFiftyFiftyUsed() ? HintType.FIFTY_FIFTY.name() : null,
                !session.getSkipQuestionUsed() ? HintType.SKIP_QUESTION.name() : null
        };
    }

    private boolean isHintAvailable(GameSession session, HintType hintType) {
        switch (hintType) {
            case FIFTY_FIFTY:
                return !session.getFiftyFiftyUsed();
            case SKIP_QUESTION:
                return !session.getSkipQuestionUsed();
            default:
                return false;
        }
    }

    public Optional<GameSessionResponse> getCurrentGame(User user) {
        Optional<GameSession> activeSession = gameSessionRepository
                .findByPlayerAndGameStatus(user, GameStatus.IN_PROGRESS);


        if (activeSession.isEmpty()) {
            return Optional.empty();
        }

        GameSession session = activeSession.get();

        // ПОЛУЧАЕМ ВОПРОС ИЗ КЭША
        Question currentQuestion = currentQuestionCache.get(session.getId());

        if (currentQuestion == null) {
            log.warn("No cached question for session {}, loading new question", session.getId());
            // Если вопроса нет в кэше, загружаем и кэшируем
            currentQuestion = getQuestionForLevel(session.getCurrentQuestionNumber());

            if (currentQuestion == null) {
                // Если не удалось получить вопрос, завершаем игру
                session.setGameStatus(GameStatus.FAILED);
                session.setFinishedAt(LocalDateTime.now());
                gameSessionRepository.save(session);
                return Optional.empty();
            }

            // Кэшируем вопрос
            currentQuestionCache.put(session.getId(), currentQuestion);
            log.info("Cached question for session {}: {}", session.getId(), currentQuestion.getQuestionText());
        } else {
            log.info("Using cached question for session {}: {}", session.getId(), currentQuestion.getQuestionText());
        }

        return Optional.of(GameSessionResponse.builder()
                .sessionId(session.getId())
                .currentQuestion(session.getCurrentQuestionNumber())
                .totalScore(session.getTotalScore())
                .question(currentQuestion) // Используем КЭШИРОВАННЫЙ вопрос
                .availableHints(getAvailableHints(session))
                .timeLimit(TIME_LIMIT_SECONDS)
                .build());
    }

    /**
     * Получить статус игровой сессии
     */
    public Optional<GameStateResponse> getGameStatus(Long sessionId, User user) {
        Optional<GameSession> sessionOpt = gameSessionRepository
                .findByIdAndPlayerIdAndGameStatus(sessionId, user.getId(), GameStatus.IN_PROGRESS);

        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        GameSession session = sessionOpt.get();

        return Optional.of(GameStateResponse.builder()
                .sessionId(sessionId)
                .gameStatus(session.getGameStatus().name())
                .currentQuestion(session.getCurrentQuestionNumber())
                .totalScore(session.getTotalScore())
                .availableHints(getAvailableHints(session))
                .startedAt(session.getStartedAt().toString())
                .timeRemaining(calculateTimeRemaining(session))
                .build());
    }

    /**
     * Получить историю игр пользователя
     */
    public Page<GameHistoryDto> getGameHistory(User user, Pageable pageable) {
        Page<GameSession> sessions = gameSessionRepository
                .findByPlayerOrderByStartedAtDesc(user, pageable);

        return sessions.map(this::convertToGameHistory);
    }

    /**
     * Получить подробную статистику игры
     */
    public GameDetailDto getGameDetail(Long sessionId, User user) {
        GameSession session = gameSessionRepository
                .findByIdAndPlayerId(sessionId, user.getId())
                .orElseThrow(() -> new RuntimeException("Игра не найдена"));

        List<GameAnswer> answers = gameAnswerRepository
                .findByGameSessionOrderByQuestionNumber(session);

        return GameDetailDto.builder()
                .sessionId(sessionId)
                .gameStatus(session.getGameStatus())
                .totalScore(session.getTotalScore())
                .questionsAnswered(session.getCurrentQuestionNumber() - 1)
                .startedAt(session.getStartedAt())
                .finishedAt(session.getFinishedAt())
                .answers(answers.stream()
                        .map(this::convertToAnswerDetail)
                        .collect(Collectors.toList()))
                .hintsUsed(getUsedHints(session))
                .build();
    }

    /**
     * Проверить можно ли возобновить игру
     */
    public boolean canResumeGame(User user) {
        return gameSessionRepository
                .findByPlayerAndGameStatus(user, GameStatus.IN_PROGRESS)
                .isPresent();
    }

// ============ Приватные вспомогательные методы ============

    private Integer calculateTimeRemaining(GameSession session) {
        // Упрощенный расчет - в реальности нужно отслеживать время начала каждого вопроса
        long secondsSinceStart = ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
        long expectedTime = (session.getCurrentQuestionNumber() - 1) * TIME_LIMIT_SECONDS;
        long remaining = expectedTime + TIME_LIMIT_SECONDS - secondsSinceStart;

        return Math.max(0, (int) remaining);
    }

    private GameHistoryDto convertToGameHistory(GameSession session) {
        return GameHistoryDto.builder()
                .sessionId(session.getId())
                .gameStatus(session.getGameStatus())
                .totalScore(session.getTotalScore())
                .questionsAnswered(session.getCurrentQuestionNumber() - 1)
                .startedAt(session.getStartedAt())
                .finishedAt(session.getFinishedAt())
                .duration(session.getFinishedAt() != null ?
                        ChronoUnit.MINUTES.between(session.getStartedAt(), session.getFinishedAt()) : null)
                .hintsUsed(getUsedHintsCount(session))
                .build();
    }

    private AnswerDetailDto convertToAnswerDetail(GameAnswer answer) {
        return AnswerDetailDto.builder()
                .questionNumber(answer.getQuestionNumber())
                .questionText(answer.getQuestionText())
                .correctAnswer(answer.getCorrectAnswer())
                .playerAnswer(answer.getPlayerAnswer())
                .isCorrect(answer.getIsCorrect())
                .pointsEarned(answer.getPointsEarned())
                .difficulty(answer.getDifficultyLevel())
                .timeTaken(answer.getTimeTakenSeconds())
                .answeredAt(answer.getAnsweredAt())
                .build();
    }

    private String[] getUsedHints(GameSession session) {
        List<String> used = new ArrayList<>();
        if (session.getFiftyFiftyUsed()) {
            used.add(HintType.FIFTY_FIFTY.name());
        }
        if (session.getSkipQuestionUsed()) {
            used.add(HintType.SKIP_QUESTION.name());
        }
        return used.toArray(new String[0]);
    }

    private int getUsedHintsCount(GameSession session) {
        int count = 0;
        if (session.getFiftyFiftyUsed()) count++;
        if (session.getSkipQuestionUsed()) count++;
        return count;
    }

    // Добавьте эти методы в ваш существующий GameService

    /**
     * Ответить на вопрос (ИСПРАВЛЕННАЯ ВЕРСИЯ)
     */
    /**
     * Ответить на вопрос (ПОЛНАЯ ИСПРАВЛЕННАЯ ВЕРСИЯ С КЭШЕМ)
     */
    public GameSessionResponse answerQuestion(Long sessionId, String playerAnswer, Long userId) {
        log.error("=== USING NEW METHOD VERSION ===");
        log.info("Processing answer for session {}, user {}, answer: {}", sessionId, userId, playerAnswer);

        // Получаем сессию
        Optional<GameSession> sessionOpt = gameSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            log.error("Session {} not found in database", sessionId);
            throw new RuntimeException("Игровая сессия не найдена");
        }

        GameSession session = sessionOpt.get();
        log.info("Found session {}: status={}, player={}, currentQuestion={}",
                sessionId, session.getGameStatus(), session.getPlayer().getId(), session.getCurrentQuestionNumber());

        // Проверяем принадлежность и статус
        if (!session.getPlayer().getId().equals(userId)) {
            log.error("Session {} belongs to user {}, but request from user {}",
                    sessionId, session.getPlayer().getId(), userId);
            throw new RuntimeException("Доступ запрещен");
        }

        if (session.getGameStatus() != GameStatus.IN_PROGRESS) {
            log.error("Session {} has status {}, expected IN_PROGRESS",
                    sessionId, session.getGameStatus());

            return GameSessionResponse.builder()
                    .sessionId(sessionId)
                    .gameOver(true)
                    .finalScore(session.getTotalScore())
                    .questionsAnswered(session.getCurrentQuestionNumber() - 1)
                    .build();
        }

        // Проверяем время
        if (isTimeExpired(session)) {
            log.info("Time expired for session {}", sessionId);
            return handleTimeExpired(session);
        }

        // ПОЛУЧАЕМ ТЕКУЩИЙ ВОПРОС ИЗ КЭША
        Question currentQuestion = currentQuestionCache.get(sessionId);
        if (currentQuestion == null) {
            log.error("No cached question for session {}, loading from provider", sessionId);
            currentQuestion = getQuestionForLevel(session.getCurrentQuestionNumber());
            if (currentQuestion == null) {
                throw new RuntimeException("Не удалось получить текущий вопрос");
            }
        } else {
            log.info("Using cached question for session {}: {}", sessionId, currentQuestion.getQuestionText());
        }

        // Проверяем ответ на КЭШИРОВАННЫЙ вопрос
        boolean isCorrect = currentQuestion.getCorrectAnswer().equals(playerAnswer);
        int pointsEarned = 0;

        log.info("Answer check: correct={}, playerAnswer='{}', correctAnswer='{}'",
                isCorrect, playerAnswer, currentQuestion.getCorrectAnswer());

        if (isCorrect) {
            DifficultyLevel level = getDifficultyForQuestion(session.getCurrentQuestionNumber());
            pointsEarned = level.getPointsPerQuestion();
            session.setTotalScore(session.getTotalScore() + pointsEarned);
            log.info("Correct answer! Earned {} points, total score: {}", pointsEarned, session.getTotalScore());
        }

        // Сохраняем ответ на КЭШИРОВАННЫЙ вопрос
        saveGameAnswer(session, currentQuestion, playerAnswer, isCorrect, pointsEarned);

        if (!isCorrect) {
            // Игра окончена - неправильный ответ
            log.info("Wrong answer! Game over for session {}", sessionId);
            session.setGameStatus(GameStatus.FAILED);
            session.setFinishedAt(LocalDateTime.now());
            gameSessionRepository.save(session);

            // УДАЛЯЕМ ИЗ КЭША
            currentQuestionCache.remove(sessionId);

            return GameSessionResponse.builder()
                    .sessionId(sessionId)
                    .gameOver(true)
                    .correctAnswer(currentQuestion.getCorrectAnswer()) // Правильный ответ КЭШИРОВАННОГО вопроса
                    .finalScore(session.getTotalScore())
                    .questionsAnswered(session.getCurrentQuestionNumber())
                    .lastAnswerCorrect(false)
                    .build();
        }

        // Переходим к следующему вопросу
        session.setCurrentQuestionNumber(session.getCurrentQuestionNumber() + 1);
        log.info("Moving to question {}", session.getCurrentQuestionNumber());

        if (session.getCurrentQuestionNumber() > MAX_QUESTIONS) {
            // Игра завершена успешно
            log.info("Game completed successfully for session {}", sessionId);
            session.setGameStatus(GameStatus.COMPLETED);
            session.setFinishedAt(LocalDateTime.now());
            gameSessionRepository.save(session);

            // УДАЛЯЕМ ИЗ КЭША
            currentQuestionCache.remove(sessionId);

            return GameSessionResponse.builder()
                    .sessionId(sessionId)
                    .gameOver(true)
                    .gameCompleted(true)
                    .finalScore(session.getTotalScore())
                    .questionsAnswered(session.getCurrentQuestionNumber() - 1)
                    .lastAnswerCorrect(true)
                    .build();
        }

        // Предварительная загрузка для следующего уровня
        DifficultyLevel currentLevel = getDifficultyForQuestion(session.getCurrentQuestionNumber());
        DifficultyLevel nextLevel = getDifficultyForQuestion(session.getCurrentQuestionNumber() + 1);

        if (!currentLevel.equals(nextLevel)) {
            log.info("Player approaching {} level, starting preload", nextLevel);
            if (questionProvider instanceof LazyOpenTDBQuestionProvider) {
                ((LazyOpenTDBQuestionProvider) questionProvider).preloadQuestionsForLevel(nextLevel);
            }
        }

        // Сохраняем сессию
        gameSessionRepository.save(session);

        // ТЕПЕРЬ получаем СЛЕДУЮЩИЙ вопрос и кэшируем его
        Question nextQuestion = getQuestionForLevel(session.getCurrentQuestionNumber());
        if (nextQuestion == null) {
            log.error("Could not get next question for level {}", session.getCurrentQuestionNumber());
            throw new RuntimeException("Не удалось получить следующий вопрос");
        }

        // КЭШИРУЕМ СЛЕДУЮЩИЙ ВОПРОС
        currentQuestionCache.put(sessionId, nextQuestion);
        log.info("Cached next question for session {}: {}", sessionId, nextQuestion.getQuestionText());

        log.info("Successfully processed answer for session {}, moving to question {}",
                sessionId, session.getCurrentQuestionNumber());

        return GameSessionResponse.builder()
                .sessionId(sessionId)
                .currentQuestion(session.getCurrentQuestionNumber())
                .totalScore(session.getTotalScore())
                .question(nextQuestion) // СЛЕДУЮЩИЙ вопрос
                .availableHints(getAvailableHints(session))
                .timeLimit(TIME_LIMIT_SECONDS)
                .lastAnswerCorrect(true)
                .pointsEarned(pointsEarned)
                .build();
    }
}
