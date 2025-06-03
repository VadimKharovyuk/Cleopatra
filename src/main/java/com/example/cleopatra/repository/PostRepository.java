package com.example.cleopatra.repository;

import com.example.cleopatra.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
