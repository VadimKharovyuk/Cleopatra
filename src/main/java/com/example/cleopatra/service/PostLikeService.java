package com.example.cleopatra.service;

import com.example.cleopatra.EVENT.PostLikedEvent;
import com.example.cleopatra.dto.Post.PostCardDto;
import com.example.cleopatra.dto.Post.PostLikeResponseDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.maper.PostMapper;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final ApplicationEventPublisher eventPublisher;



    /**
     * Лайкнуть/убрать лайк с поста
     */

    /**
     * Лайкнуть/убрать лайк с поста
     */
    @Transactional
    public PostLikeResponseDto toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        boolean isLiked = post.getLikedBy().contains(user);

        if (isLiked) {
            // Убираем лайк
            post.getLikedBy().remove(user);
            user.getLikedPosts().remove(post);
            log.info("Пользователь {} убрал лайк с поста {}", userId, postId);
        } else {
            // Добавляем лайк
            post.getLikedBy().add(user);
            user.getLikedPosts().add(post);
            log.info("Пользователь {} лайкнул пост {}", userId, postId);

            // 🔥 ПУБЛИКУЕМ СОБЫТИЕ ТОЛЬКО ПРИ ДОБАВЛЕНИИ ЛАЙКА
            eventPublisher.publishEvent(new PostLikedEvent(
                    postId,
                    post.getAuthor().getId(), // postAuthorId
                    userId,                   // likerUserId
                    post.getContent() != null ? post.getContent() : "Без названия"
            ));
        }

        // Обновляем счетчик лайков
        post.setLikesCount((long) post.getLikedBy().size());
        postRepository.save(post);

        return PostLikeResponseDto.builder()
                .postId(postId)
                .isLiked(!isLiked)
                .likesCount(post.getLikesCount())
                .build();
    }

    /**
     * Проверяет, лайкнул ли пользователь пост
     */
    public Boolean isPostLikedByUser(Post post, Long currentUserId) {
        if (currentUserId == null) return false;

        return post.getLikedBy().stream()
                .anyMatch(user -> user.getId().equals(currentUserId));
    }

    /**
     * Получает последних пользователей, которые лайкнули пост (для PostResponseDto)
     */
    public List<PostResponseDto.LikeUserDto> getRecentLikes(Post post, int limit) {
        return post.getLikedBy().stream()
                .limit(limit)
                .map(postMapper::toLikeUserDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает последних пользователей, которые лайкнули пост (для PostCardDto)
     */
    public List<PostCardDto.LikeUserDto> getRecentLikesForCard(Post post, int limit) {
        return post.getLikedBy().stream()
                .limit(limit)
                .map(postMapper::toCardLikeUserDto)
                .collect(Collectors.toList());
    }

}