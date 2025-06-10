package com.example.cleopatra.repository;

import com.example.cleopatra.model.StoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    Page<StoryView> findByStoryId(Long storyId, Pageable pageable);
    boolean existsByStoryIdAndViewerId(Long storyId, Long viewerId);
    Optional<StoryView> findByStoryIdAndViewerId(Long storyId, Long viewerId);
    Long countByStoryId(Long storyId);
    int deleteByStoryId(Long storyId);

    @Query("SELECT sv FROM StoryView sv " +
            "JOIN FETCH sv.viewer " +
            "WHERE sv.story.id = :storyId " +
            "ORDER BY sv.viewedAt DESC")
    Page<StoryView> findByStoryIdWithViewer(@Param("storyId") Long storyId, Pageable pageable);

    @Query("SELECT sv FROM StoryView sv " +
            "JOIN FETCH sv.viewer " +
            "WHERE sv.story.id = :storyId " +
            "ORDER BY sv.viewedAt DESC")
    List<StoryView> findByStoryIdWithViewer(@Param("storyId") Long storyId);

    // Оптимизированный метод для получения ограниченного количества с информацией о пользователе
    @Query(value = "SELECT sv.* FROM story_views sv " +
            "JOIN users u ON sv.viewer_id = u.id " +
            "WHERE sv.story_id = :storyId " +
            "ORDER BY sv.viewed_at DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<StoryView> findTopByStoryIdWithViewer(@Param("storyId") Long storyId, @Param("limit") int limit);
}