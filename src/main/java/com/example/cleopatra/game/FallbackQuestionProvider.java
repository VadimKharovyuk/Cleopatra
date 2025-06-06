package com.example.cleopatra.game;

import com.example.cleopatra.enums.DifficultyLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class FallbackQuestionProvider implements QuestionProvider {

    private final Map<DifficultyLevel, List<Question>> localQuestions;

    public FallbackQuestionProvider() {
        this.localQuestions = initializeLocalQuestions();
    }

    @Override
    public Question getQuestion(DifficultyLevel difficulty) {
        List<Question> questions = localQuestions.get(difficulty);
        if (questions != null && !questions.isEmpty()) {
            return questions.get(new Random().nextInt(questions.size()));
        }
        return createDefaultQuestion(difficulty);
    }

    @Override
    public List<Question> getQuestions(DifficultyLevel difficulty, int amount) {
        List<Question> questions = localQuestions.get(difficulty);
        List<Question> result = new ArrayList<>();

        if (questions != null && !questions.isEmpty()) {
            Random random = new Random();
            for (int i = 0; i < amount; i++) {
                result.add(questions.get(random.nextInt(questions.size())));
            }
        } else {
            for (int i = 0; i < amount; i++) {
                result.add(createDefaultQuestion(difficulty));
            }
        }

        return result;
    }

    @Override
    public boolean isAvailable() {
        return true; // Локальные вопросы всегда доступны
    }

    private Map<DifficultyLevel, List<Question>> initializeLocalQuestions() {
        Map<DifficultyLevel, List<Question>> questions = new HashMap<>();

        // Легкие вопросы
        List<Question> easyQuestions = Arrays.asList(
                createQuestion("Какой цвет получается при смешивании красного и синего?",
                        "Фиолетовый", Arrays.asList("Зеленый", "Желтый", "Оранжевый"), DifficultyLevel.EASY),
                createQuestion("Сколько дней в году (не високосном)?",
                        "365", Arrays.asList("364", "366", "360"), DifficultyLevel.EASY),
                createQuestion("Какая планета ближайшая к Солнцу?",
                        "Меркурий", Arrays.asList("Венера", "Земля", "Марс"), DifficultyLevel.EASY)
        );

        // Средние вопросы
        List<Question> mediumQuestions = Arrays.asList(
                createQuestion("В каком году была основана компания Microsoft?",
                        "1975", Arrays.asList("1976", "1974", "1977"), DifficultyLevel.MEDIUM),
                createQuestion("Какой химический символ у золота?",
                        "Au", Arrays.asList("Go", "Gd", "Ag"), DifficultyLevel.MEDIUM),
                createQuestion("Кто написал роман '1984'?",
                        "Джордж Оруэлл", Arrays.asList("Олдос Хаксли", "Рэй Брэдбери", "Айзек Азимов"), DifficultyLevel.MEDIUM)
        );

        // Сложные вопросы
        List<Question> hardQuestions = Arrays.asList(
                createQuestion("Какая константа определяет скорость света в вакууме?",
                        "c", Arrays.asList("h", "G", "k"), DifficultyLevel.HARD),
                createQuestion("В каком году была подписана Великая хартия вольностей?",
                        "1215", Arrays.asList("1216", "1214", "1220"), DifficultyLevel.HARD),
                createQuestion("Кто разработал теорию относительности?",
                        "Альберт Эйнштейн", Arrays.asList("Исаак Ньютон", "Макс Планк", "Нильс Бор"), DifficultyLevel.HARD)
        );

        questions.put(DifficultyLevel.EASY, easyQuestions);
        questions.put(DifficultyLevel.MEDIUM, mediumQuestions);
        questions.put(DifficultyLevel.HARD, hardQuestions);
        questions.put(DifficultyLevel.GENIUS, hardQuestions); // Используем сложные для гениального уровня

        return questions;
    }

    private Question createQuestion(String questionText, String correctAnswer,
                                    List<String> incorrectAnswers, DifficultyLevel difficulty) {
        List<String> allAnswers = new ArrayList<>();
        allAnswers.add(correctAnswer);
        allAnswers.addAll(incorrectAnswers);
        Collections.shuffle(allAnswers);

        return Question.builder()
                .questionText(questionText)
                .correctAnswer(correctAnswer)
                .incorrectAnswers(incorrectAnswers)
                .allAnswers(allAnswers)
                .correctAnswerIndex(allAnswers.indexOf(correctAnswer))
                .category("General Knowledge")
                .difficulty(difficulty)
                .build();
    }

    private Question createDefaultQuestion(DifficultyLevel difficulty) {
        return createQuestion(
                "Локальный вопрос уровня " + difficulty + ". Правильный ответ: A",
                "A",
                Arrays.asList("B", "C", "D"),
                difficulty
        );
    }
}