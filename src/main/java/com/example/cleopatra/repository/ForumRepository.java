package com.example.cleopatra.repository;

import com.example.cleopatra.enums.ForumType;
import com.example.cleopatra.model.Forum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface ForumRepository extends JpaRepository<Forum, Long> {

    // ✅ Кастомный метод с @Query (работает)
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT f FROM Forum f WHERE f.id = :id")
    Optional<Forum> findByIdWithUser(@Param("id") Long id);

    // ✅ Переопределяем стандартные методы JPA
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Forum> findAll(Pageable pageable);

    // ✅ Стандартные методы JPA (убрали WithUser из названий!)
    @EntityGraph(attributePaths = {"user"})
    Page<Forum> findByForumType(ForumType forumType, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Forum> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description, Pageable pageable);

    // ✅ Метод подсчета
    long countByForumType(ForumType forumType);

    // ✅ Обновление счетчика
    @Modifying
    @Query("UPDATE Forum f SET f.viewCount = f.viewCount + 1 WHERE f.id = :forumId")
    void incrementViewCount(@Param("forumId") Long forumId);



}

