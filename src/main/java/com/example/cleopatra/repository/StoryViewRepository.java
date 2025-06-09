package com.example.cleopatra.repository;

import com.example.cleopatra.model.StoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    Page<StoryView> findByStoryId(Long storyId, Pageable pageable);
    boolean existsByStoryIdAndViewerId(Long storyId, Long viewerId);
    Optional<StoryView> findByStoryIdAndViewerId(Long storyId, Long viewerId);
    Long countByStoryId(Long storyId);
    int deleteByStoryId(Long storyId);
}