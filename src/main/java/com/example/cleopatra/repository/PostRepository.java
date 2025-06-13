package com.example.cleopatra.repository;

import com.example.cleopatra.model.Post;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Найти посты пользователя (неудаленные) с пагинацией
     */
    Slice<Post> findByAuthor_IdAndIsDeletedFalse(Long authorId, Pageable pageable);

    /**
     * Найти все посты пользователя (включая удаленные) - для админки
     */
    Slice<Post> findByAuthor_Id(Long authorId, Pageable pageable);

    /**
     * Найти посты от списка авторов (подписки)
     */
    Slice<Post> findByAuthor_IdInAndIsDeletedFalse(List<Long> authorIds, Pageable pageable);

    /**
     * Популярные посты для рекомендаций
     */
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false ORDER BY p.likesCount DESC, p.createdAt DESC")
    Slice<Post> findByIsDeletedFalseOrderByLikesCountDescCreatedAtDesc(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM posts WHERE user_id = :userId", nativeQuery = true)
    Long countByAuthorId(@Param("userId") Long userId);


    boolean existsByAuthorIdAndLikesCountGreaterThan(Long authorId, Long likesCount);

    Long countByAuthorIdAndImageUrlIsNotNull(Long authorId);


    // PostRepository - дополнительные полезные методы для агрегации
    @Query("SELECT SUM(p.likesCount) FROM Post p WHERE p.author.id = :authorId AND p.isDeleted = false")
    Long sumLikesByAuthor(@Param("authorId") Long authorId);

    @Query("SELECT SUM(p.commentsCount) FROM Post p WHERE p.author.id = :authorId AND p.isDeleted = false")
    Long sumCommentsByAuthor(@Param("authorId") Long authorId);

    @Query("SELECT SUM(p.viewsCount) FROM Post p WHERE p.author.id = :authorId AND p.isDeleted = false")
    Long sumViewsByAuthor(@Param("authorId") Long authorId);

    @Query("SELECT p FROM Post p " +
            "WHERE p.author.id = :userId AND (p.isDeleted = false OR p.isDeleted IS NULL) " +
            "ORDER BY p.likesCount DESC")
    List<Post> findTopPostsByUser(@Param("userId") Long userId, Pageable pageable);


    default List<Post> findTopPostsByUser(Long userId, int limit) {
        return findTopPostsByUser(userId, PageRequest.of(0, limit));
    }



    List<Post> findByAuthorId(Long userId);


    List<Post> findByAuthorIdAndCreatedAtAfter(Long userId, LocalDateTime weekAgo);


    @Query("SELECT SUM(p.likesCount) FROM Post p WHERE p.author.id = :userId")
    Long getTotalLikesByAuthor(@Param("userId") Long userId);
}
