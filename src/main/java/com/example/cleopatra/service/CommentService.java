package com.example.cleopatra.service;

import com.example.cleopatra.dto.Comment.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Интерфейс сервиса для работы с комментариями
 */
public interface CommentService {

    /**
     * Получить комментарии к посту с пагинацией
     *
     * @param postId   ID поста
     * @param pageable параметры пагинации
     * @return страница комментариев
     */
    CommentPageResponse getCommentsByPost(Long postId, Pageable pageable);

    /**
     * Создать новый комментарий
     *
     * @param postId  ID поста
     * @param request данные для создания комментария
     * @param userEmail email пользователя (из SecurityContext)
     * @return созданный комментарий
     */
    CommentResponse createComment(Long postId, CreateCommentRequest request, String userEmail);

    /**
     * Обновить существующий комментарий
     *
     * @param commentId ID комментария
     * @param request   данные для обновления
     * @param userEmail email пользователя
     * @return обновленный комментарий
     */
    CommentResponse updateComment(Long commentId, UpdateCommentRequest request, String userEmail);

    /**
     * Удалить комментарий (мягкое удаление)
     *
     * @param commentId ID комментария
     * @param userEmail email пользователя
     * @return результат операции
     */
    CommentActionResponse deleteComment(Long commentId, String userEmail);

    /**
     * Получить комментарий по ID
     *
     * @param commentId ID комментария
     * @return комментарий
     */
    CommentResponse getCommentById(Long commentId);

    /**
     * Получить комментарии пользователя
     *
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return страница комментариев пользователя
     */
    CommentPageResponse getUserComments(Long userId, Pageable pageable);

    /**
     * Получить последние комментарии к посту (для предпросмотра)
     *
     * @param postId ID поста
     * @param limit  количество комментариев
     * @return список последних комментариев
     */
    List<CommentResponse> getLatestComments(Long postId, int limit);

    /**
     * Получить количество комментариев к посту
     *
     * @param postId ID поста
     * @return количество комментариев
     */
    long getCommentsCount(Long postId);
}
