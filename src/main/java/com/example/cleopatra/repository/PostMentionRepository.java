package com.example.cleopatra.repository;

import com.example.cleopatra.model.PostMention;
import com.example.cleopatra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostMentionRepository extends JpaRepository<PostMention, Long> {

    List<PostMention> findByMentionedUserOrderByCreatedAtDesc(User mentionedUser);

    List<PostMention> findByPostId(Long postId);

    boolean existsByPostIdAndMentionedUserId(Long postId, Long userId);
}
