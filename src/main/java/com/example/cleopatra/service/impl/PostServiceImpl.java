package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.PostNotFoundException;
import com.example.cleopatra.dto.Post.*;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.maper.PostMapper;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.CommentRepository;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final StorageService storageService;
    private final ImageValidator imageValidator;
    private final PostMapper postMapper;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final PostLikeService postLikeService;
    private final CommentService commentService;

    @Override
    public PostResponseDto createPost(PostCreateDto postCreateDto) {
        log.info("Создание нового поста");

        User currentUser = getCurrentUser();
        Post post = postMapper.toEntity(postCreateDto, currentUser);

        if (postCreateDto.getImage() != null && !postCreateDto.getImage().isEmpty()) {
            try {
                ImageConverterService.ProcessedImage processedImage =
                        imageValidator.validateAndProcess(postCreateDto.getImage());

                StorageService.StorageResult storageResult =
                        storageService.uploadProcessedImage(processedImage);

                post.setImageUrl(storageResult.getUrl());
                post.setImgId(storageResult.getImageId());

                log.info("Изображение успешно загружено: {}", storageResult.getImageId());

            } catch (Exception e) {
                log.error("Ошибка при загрузке изображения: {}", e.getMessage());
                throw new RuntimeException("Не удалось загрузить изображение: " + e.getMessage());
            }
        }

        Post savedPost = postRepository.save(post);
        userRepository.save(currentUser);

        log.info("Пост успешно создан с ID: {}", savedPost.getId());

        // ✅ ОБНОВЛЕННЫЙ ВЫЗОВ с логикой лайков
        Boolean isLiked = postLikeService.isPostLikedByUser(savedPost, currentUser.getId());
        List<PostResponseDto.LikeUserDto> recentLikes =
                postLikeService.getRecentLikes(savedPost, 5);

        return postMapper.toResponseDto(savedPost, isLiked, recentLikes);
    }

    @Override
    public PostResponseDto getPostById(Long id) {
        log.info("Получение поста с ID: {}", id);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пост с ID " + id + " не найден"));

        // Увеличиваем счетчик просмотров
        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);

        Long commentsCount = commentService.getCommentsCount(post.getId());
        post.setCommentsCount(commentsCount);

        User currentUser = getCurrentUser();

        log.info("Пост найден: {}", post.getContent().substring(0, Math.min(50, post.getContent().length())));

        // ✅ ОБНОВЛЕННЫЙ ВЫЗОВ с логикой лайков
        Boolean isLiked = postLikeService.isPostLikedByUser(post, currentUser.getId());
        List<PostResponseDto.LikeUserDto> recentLikes =
                postLikeService.getRecentLikes(post, 5);

        return postMapper.toResponseDto(post, isLiked, recentLikes);
    }

    @Override
    public PostListDto getUserPosts(Long userId, int page, int size) {
//        log.info("Получение постов пользователя с ID: {}, страница: {}, размер: {}", userId, page, size);

        if (!userService.userExists(userId)) {
            throw new RuntimeException("Пользователь с ID " + userId + " не найден");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Slice<Post> postSlice = postRepository.findByAuthor_IdAndIsDeletedFalse(userId, pageable);

//        log.info("Найдено {} постов для пользователя {}", postSlice.getNumberOfElements(), userId);

        // ✅ ОБНОВЛЕННЫЙ МЕТОД с логикой лайков
        return convertPostSliceToListDto(postSlice, page);
    }

    @Override
    public PostListDto getMyPosts(int page, int size) {
        log.info("Получение собственных постов, страница: {}, размер: {}", page, size);
        User currentUser = getCurrentUser();
        return getUserPosts(currentUser.getId(), page, size);
    }

    @Override
    public PostListDto getFeedPosts(Long userId, int page, int size) {
        log.info("Получение ленты новостей для пользователя: {}, страница: {}, размер: {}", userId, page, size);

        List<Long> subscriptionIds = subscriptionService.getSubscriptionIds(userId);

        if (subscriptionIds.isEmpty()) {
            log.info("У пользователя {} нет подписок, показываем рекомендованные посты", userId);
            return getRecommendedPosts(userId, page, size);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Slice<Post> postSlice = postRepository.findByAuthor_IdInAndIsDeletedFalse(subscriptionIds, pageable);

        log.info("Найдено {} постов в ленте для пользователя {}", postSlice.getNumberOfElements(), userId);

        // ✅ ОБНОВЛЕННЫЙ МЕТОД с логикой лайков
        return convertPostSliceToListDto(postSlice, page);
    }

    @Override
    public PostListDto getRecommendedPosts(Long userId, int page, int size) {
        log.info("Получение рекомендованных постов для пользователя: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Slice<Post> postSlice = postRepository.findByIsDeletedFalseOrderByLikesCountDescCreatedAtDesc(pageable);

        // ✅ ОБНОВЛЕННЫЙ МЕТОД с логикой лайков
        return convertPostSliceToListDto(postSlice, page);
    }

    // ✅ НОВЫЕ МЕТОДЫ для работы с лайками

    /**
     * Лайкнуть/убрать лайк с поста
     */
    @Override
    public PostLikeResponseDto toggleLike(Long postId) {
        User currentUser = getCurrentUser();
        return postLikeService.toggleLike(postId, currentUser.getId());
    }

    /**
     * Получить информацию о лайках поста
     */
    @Override
    public PostLikeInfoDto getLikeInfo(Long postId) {
        User currentUser = getCurrentUser();
        Post post = findById(postId);

        Boolean isLiked = postLikeService.isPostLikedByUser(post, currentUser.getId());
        List<PostResponseDto.LikeUserDto> recentLikes =
                postLikeService.getRecentLikes(post, 10);

        return PostLikeInfoDto.builder()
                .postId(postId)
                .likesCount(post.getLikesCount())
                .isLikedByCurrentUser(isLiked)
                .recentLikes(recentLikes)
                .build();
    }

    // ✅ ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ

    /**
     * Конвертирует Slice<Post> в PostListDto с логикой лайков
     */
    private PostListDto convertPostSliceToListDto(Slice<Post> postSlice, int page) {
        User currentUser = getCurrentUser();

        List<PostCardDto> postCards = postSlice.getContent().stream()
                .map(post -> {
                    Boolean isLiked = postLikeService.isPostLikedByUser(post, currentUser.getId());
                    List<PostCardDto.LikeUserDto> recentLikes =
                            postLikeService.getRecentLikesForCard(post, 3);

                    return postMapper.toCardDto(post, isLiked, recentLikes);
                })
                .collect(Collectors.toList());

        return postMapper.toListDto(
                postCards,
                page,
                postSlice.hasNext(),
                postSlice.getSize()
        );
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Post post = findById(postId);
        Long userId = post.getAuthor().getId();

        log.info("🗑️ Удаляем пост {} пользователя {}", postId, userId);

        Long countBefore = postRepository.countByAuthorId(userId);
        log.info("📊 Количество постов ДО удаления: {}", countBefore);

        postRepository.deleteById(postId);


        Long countAfter = postRepository.countByAuthorId(userId);
        log.info("📊 Количество постов ПОСЛЕ удаления: {}", countAfter);
    }

    @Override
    public Post findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Пост с ID " + postId + " не найден"));
    }

    private User getCurrentUser() {
        return userService.getCurrentUserEntity();
    }
}