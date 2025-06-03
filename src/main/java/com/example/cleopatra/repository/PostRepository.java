package com.example.cleopatra.repository;

import com.example.cleopatra.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
