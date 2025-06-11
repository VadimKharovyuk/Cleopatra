package com.example.cleopatra.service.impl;

import com.example.cleopatra.EVENT.PostCommentEvent;
import com.example.cleopatra.ExistsException.CommentNotFoundException;
import com.example.cleopatra.ExistsException.PostNotFoundException;
import com.example.cleopatra.ExistsException.UnauthorizedException;
import com.example.cleopatra.dto.AICommentResponse;
import com.example.cleopatra.dto.Comment.*;
import com.example.cleopatra.dto.CreateCommentWithAIRequest;
import com.example.cleopatra.dto.ImproveCommentRequest;
import com.example.cleopatra.maper.CommentMapper;
import com.example.cleopatra.model.Comment;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.CommentRepository;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.CommentAIService;
import com.example.cleopatra.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentAIService commentAIService;

    @Override
    public CommentPageResponse getCommentsByPost(Long postId, Pageable pageable) {
        log.debug("Получение комментариев для поста с ID: {}, страница: {}", postId, pageable.getPageNumber());

        // Проверяем существование поста
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException("Пост с ID " + postId + " не найден");
        }

        // Получаем комментарии с пагинацией
        Slice<Comment> commentSlice = commentRepository.findByPostIdWithAuthor(postId, pageable);

        // Получаем общее количество комментариев для метаинформации
        long totalComments = commentRepository.countByPostIdAndIsDeletedFalse(postId);

        return commentMapper.toCommentPageResponse(commentSlice, totalComments);
    }






    /**
     * 🤖 НОВЫЙ: Создание комментария с помощью AI
     */
    @Override
    @Transactional
    public CommentResponse createCommentWithAI(Long postId, CreateCommentWithAIRequest request, String userEmail) {
        log.debug("Создание AI комментария к посту {} пользователем {} с промптом: {}",
                postId, userEmail, request.getPrompt());

        // Проверяем доступность AI сервиса
        if (!commentAIService.isAIServiceAvailable()) {
            throw new RuntimeException("AI сервис недоступен. Попробуйте создать комментарий вручную.");
        }

        long startTime = System.currentTimeMillis();

        // Находим пост
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Пост с ID " + postId + " не найден"));

        // Находим пользователя
        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с email " + userEmail + " не найден"));

        try {
            // Генерируем комментарий с помощью AI
            String generatedComment = commentAIService.generateComment(
                    buildPromptWithType(request), post);

            // Создаем комментарий
            Comment comment = Comment.builder()
                    .content(generatedComment)
                    .post(post)
                    .author(author)
                    .isDeleted(false)
                    .build();

            // Сохраняем комментарий
            Comment savedComment = commentRepository.save(comment);

            // Публикуем событие
            eventPublisher.publishEvent(new PostCommentEvent(
                    post.getId(),
                    post.getAuthor().getId(),
                    author.getId(),
                    savedComment.getId(),
                    generatedComment
            ));

            long endTime = System.currentTimeMillis();
            log.info("AI комментарий с ID {} успешно создан пользователем {} за {} мс",
                    savedComment.getId(), userEmail, endTime - startTime);

            return commentMapper.toCommentResponse(savedComment);

        } catch (Exception e) {
            log.error("Ошибка при создании AI комментария: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать комментарий с помощью AI: " + e.getMessage());
        }
    }

    /**
     * 🤖 НОВЫЙ: Генерация комментария без сохранения (для предпросмотра)
     */
    @Override
    public AICommentResponse generateCommentPreview(Long postId, CreateCommentWithAIRequest request) {
        log.debug("Генерация превью комментария для поста {} с промптом: {}", postId, request.getPrompt());

        if (!commentAIService.isAIServiceAvailable()) {
            return AICommentResponse.builder()
                    .success(false)
                    .errorMessage("AI сервис недоступен")
                    .build();
        }

        long startTime = System.currentTimeMillis();

        try {
            // Находим пост
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException("Пост с ID " + postId + " не найден"));

            // Генерируем комментарий
            String generatedComment = commentAIService.generateComment(
                    buildPromptWithType(request), post);

            long endTime = System.currentTimeMillis();

            return AICommentResponse.builder()
                    .generatedComment(generatedComment)
                    .usedPrompt(request.getPrompt())
                    .generationTimeMs(endTime - startTime)
                    .success(true)
                    .additionalInfo("Комментарий сгенерирован для превью. Нажмите 'Опубликовать' для сохранения.")
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при генерации превью комментария: {}", e.getMessage());
            return AICommentResponse.builder()
                    .success(false)
                    .errorMessage("Ошибка генерации: " + e.getMessage())
                    .usedPrompt(request.getPrompt())
                    .build();
        }
    }

    /**
     * 🤖 НОВЫЙ: Улучшение существующего комментария
     */
    @Override
    public AICommentResponse improveComment(Long postId, ImproveCommentRequest request) {
        log.debug("Улучшение комментария для поста {}", postId);

        if (!commentAIService.isAIServiceAvailable()) {
            return AICommentResponse.builder()
                    .success(false)
                    .errorMessage("AI сервис недоступен")
                    .build();
        }

        long startTime = System.currentTimeMillis();

        try {
            // Находим пост
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException("Пост с ID " + postId + " не найден"));

            // Улучшаем комментарий
            String improvedComment = commentAIService.improveComment(
                    request.getOriginalComment(), post);

            long endTime = System.currentTimeMillis();

            return AICommentResponse.builder()
                    .generatedComment(improvedComment)
                    .usedPrompt("Улучшение: " + request.getImprovementType().getDescription())
                    .generationTimeMs(endTime - startTime)
                    .success(true)
                    .additionalInfo("Комментарий улучшен с помощью AI")
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при улучшении комментария: {}", e.getMessage());
            return AICommentResponse.builder()
                    .success(false)
                    .errorMessage("Ошибка улучшения: " + e.getMessage())
                    .generatedComment(request.getOriginalComment()) // Возвращаем оригинал
                    .build();
        }
    }

    /**
     * Строит промпт с учетом типа комментария
     */
    private String buildPromptWithType(CreateCommentWithAIRequest request) {
        StringBuilder prompt = new StringBuilder();

        // Добавляем тип комментария к промпту
        switch (request.getCommentType()) {
            case QUESTION:
                prompt.append("Задай вопрос автору поста: ");
                break;
            case POSITIVE:
                prompt.append("Напиши положительный комментарий: ");
                break;
            case CONSTRUCTIVE:
                prompt.append("Напиши конструктивную критику: ");
                break;
            case TECHNICAL:
                prompt.append("Напиши технический комментарий: ");
                break;
            case CREATIVE:
                prompt.append("Напиши креативный комментарий: ");
                break;
            default:
                // Для GENERAL ничего не добавляем
                break;
        }

        prompt.append(request.getPrompt());

        // Добавляем дополнительный контекст, если есть
        if (request.getAdditionalContext() != null && !request.getAdditionalContext().trim().isEmpty()) {
            prompt.append(". Дополнительно: ").append(request.getAdditionalContext().trim());
        }

        return prompt.toString();
    }
    @Override
    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request, String userEmail) {
        log.debug("Создание комментария к посту {} пользователем {}", postId, userEmail);

        // Находим пост
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Пост с ID " + postId + " не найден"));

        // Находим пользователя
        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с email " + userEmail + " не найден"));

        // Создаем комментарий
        Comment comment = Comment.builder()
                .content(request.getContent().trim())
                .post(post)
                .author(author)
                .isDeleted(false)
                .build();



        // Сохраняем комментарий
        Comment savedComment = commentRepository.save(comment);

        // ✅ Публикуем событие ПОСЛЕ сохранения комментария
        eventPublisher.publishEvent(new PostCommentEvent(
                post.getId(),                    // postId
                post.getAuthor().getId(),        // postAuthorId (кому отправить уведомление)
                author.getId(),                  // commenterUserId (кто прокомментировал)
                savedComment.getId(),            // commentId
                request.getContent().trim()      // commentText (для превью)
        ));

        log.info("Комментарий с ID {} успешно создан пользователем {}", savedComment.getId(), userEmail);

        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, String userEmail) {
        log.debug("Обновление комментария {} пользователем {}", commentId, userEmail);

        // Находим комментарий
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Комментарий с ID " + commentId + " не найден"));

        // Проверяем права доступа
        if (!comment.getAuthor().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("У вас нет прав для редактирования этого комментария");
        }

        // Обновляем содержимое
        comment.setContent(request.getContent().trim());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);

        log.info("Комментарий с ID {} успешно обновлен", commentId);

        return commentMapper.toCommentResponse(updatedComment);
    }

    @Override
    @Transactional
    public CommentActionResponse deleteComment(Long commentId, String userEmail) {
        log.debug("Удаление комментария {} пользователем {}", commentId, userEmail);

        // Находим комментарий
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Комментарий с ID " + commentId + " не найден"));

        // Проверяем права доступа
        if (!comment.getAuthor().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("У вас нет прав для удаления этого комментария");
        }

        // Мягкое удаление
        comment.setIsDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());

        commentRepository.save(comment);

        log.info("Комментарий с ID {} успешно удален", commentId);

        return CommentActionResponse.builder()
                .success(true)
                .message("Комментарий успешно удален")
                .build();
    }

    @Override
    public CommentResponse getCommentById(Long commentId) {
        log.debug("Получение комментария с ID: {}", commentId);

        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Комментарий с ID " + commentId + " не найден"));

        return commentMapper.toCommentResponse(comment);
    }

    @Override
    public CommentPageResponse getUserComments(Long userId, Pageable pageable) {
        log.debug("Получение комментариев пользователя с ID: {}", userId);

        // Проверяем существование пользователя
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("Пользователь с ID " + userId + " не найден");
        }

        Slice<Comment> commentSlice = commentRepository.findByAuthorIdAndIsDeletedFalse(userId, pageable);

        // Для пользовательских комментариев можем не считать общее количество для производительности
        long totalComments = 0L; // Или можно добавить отдельный метод подсчета в репозиторий

        return commentMapper.toCommentPageResponse(commentSlice, totalComments);
    }

    @Override
    public List<CommentResponse> getLatestComments(Long postId, int limit) {
        log.debug("Получение {} последних комментариев для поста {}", limit, postId);

        // Проверяем существование поста
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException("Пост с ID " + postId + " не найден");
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<Comment> comments = commentRepository.findTop3ByPostIdAndIsDeletedFalse(postId, pageable);

        return commentMapper.toCommentResponseList(comments);
    }

    @Override
    public long getCommentsCount(Long postId) {
        log.debug("Подсчет комментариев для поста {}", postId);

        // Проверяем существование поста
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException("Пост с ID " + postId + " не найден");
        }

        return commentRepository.countByPostIdAndIsDeletedFalse(postId);
    }
}
