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

    @EntityGraph(attributePaths = {"user"})
    Page<Forum> findByForumType(ForumType forumType, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Forum> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description, Pageable pageable);

    @Modifying
    @Query("UPDATE Forum f SET f.viewCount = f.viewCount + 1 WHERE f.id = :forumId")
    void incrementViewCount(@Param("forumId") Long forumId);


    @EntityGraph(attributePaths = {"user"})
    Optional<Forum> findByIdWithUser(Long id); // ✅ Более явно и безопасно
}

