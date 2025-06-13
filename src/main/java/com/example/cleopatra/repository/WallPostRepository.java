package com.example.cleopatra.repository;

import com.example.cleopatra.model.WallPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// В WallPostRepository изменить методы:
public interface WallPostRepository extends JpaRepository<WallPost, Long> {

    // Вместо findByIdAndIsDeletedFalse
    Optional<WallPost> findById(Long id);

    // Вместо findByWallOwnerIdAndIsDeletedFalse
    Slice<WallPost> findByWallOwnerId(Long wallOwnerId, Pageable pageable);

    // Или добавить кастомные запросы:
    @Query("SELECT w FROM WallPost w WHERE w.wallOwner.id = :wallOwnerId ORDER BY w.createdAt DESC")
    Slice<WallPost> findWallPosts(@Param("wallOwnerId") Long wallOwnerId, Pageable pageable);

    Optional<WallPost> findByIdAndIsDeletedFalse(Long id);
}