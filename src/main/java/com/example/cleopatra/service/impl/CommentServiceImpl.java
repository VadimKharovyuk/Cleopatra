package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.CommentNotFoundException;
import com.example.cleopatra.ExistsException.PostNotFoundException;
import com.example.cleopatra.ExistsException.UnauthorizedException;
import com.example.cleopatra.dto.Comment.*;
import com.example.cleopatra.maper.CommentMapper;
import com.example.cleopatra.model.Comment;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.CommentRepository;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
