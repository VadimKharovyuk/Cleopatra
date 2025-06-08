package com.example.cleopatra.repository;

import com.example.cleopatra.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Получить комментарии к посту с пагинацией (Slice для автоподгрузки)
     * Сортировка по дате создания (новые сверху)
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.post.id = :postId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt DESC")
    Slice<Comment> findByPostIdAndIsDeletedFalse(@Param("postId") Long postId, Pageable pageable);

    /**
     * Получить комментарии к посту с пагинацией (старые сверху)
     * Полезно для некоторых UI паттернов
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.post.id = :postId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt ASC")
    Slice<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(@Param("postId") Long postId, Pageable pageable);

    /**
     * Подсчет общего количества комментариев к посту (не удаленных)
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.post.id = :postId " +
            "AND c.isDeleted = false")
    long countByPostIdAndIsDeletedFalse(@Param("postId") Long postId);

    /**
     * Получить комментарий по ID, если он не удален
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.id = :commentId " +
            "AND c.isDeleted = false")
    Optional<Comment> findByIdAndIsDeletedFalse(@Param("commentId") Long commentId);

    /**
     * Получить все комментарии пользователя (не удаленные)
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.author.id = :userId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt DESC")
    Slice<Comment> findByAuthorIdAndIsDeletedFalse(@Param("userId") Long userId, Pageable pageable);

    /**
     * Получить последние комментарии к посту (для предпросмотра)
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.post.id = :postId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findTop3ByPostIdAndIsDeletedFalse(@Param("postId") Long postId, Pageable pageable);

    /**
     * Проверить, принадлежит ли комментарий пользователю
     */
    @Query("SELECT COUNT(c) > 0 FROM Comment c " +
            "WHERE c.id = :commentId " +
            "AND c.author.id = :userId " +
            "AND c.isDeleted = false")
    boolean existsByIdAndAuthorIdAndIsDeletedFalse(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * Получить комментарии к посту с подгрузкой автора (для оптимизации N+1)
     */
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "WHERE c.post.id = :postId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt DESC")
    Slice<Comment> findByPostIdWithAuthor(@Param("postId") Long postId, Pageable pageable);
}