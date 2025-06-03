package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.PostNotFoundException;
import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.maper.PostMapper;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
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

    @Override
    public PostResponseDto createPost(PostCreateDto postCreateDto) {
        log.info("Создание нового поста");

        // Получаем текущего пользователя
        User currentUser = getCurrentUser();

        // Создаем пост через маппер
        Post post = postMapper.toEntity(postCreateDto, currentUser);

        // Обрабатываем изображение, если оно есть
        if (postCreateDto.getImage() != null && !postCreateDto.getImage().isEmpty()) {
            try {
                // Валидация и обработка изображения
                ImageConverterService.ProcessedImage processedImage =
                        imageValidator.validateAndProcess(postCreateDto.getImage());

                // Загрузка в хранилище
                StorageService.StorageResult storageResult =
                        storageService.uploadProcessedImage(processedImage);

                // Устанавливаем URL и ID изображения
                post.setImageUrl(storageResult.getUrl());
                post.setImgId(storageResult.getImageId());

                log.info("Изображение успешно загружено: {}", storageResult.getImageId());

            } catch (Exception e) {
                log.error("Ошибка при загрузке изображения: {}", e.getMessage());
                throw new RuntimeException("Не удалось загрузить изображение: " + e.getMessage());
            }
        }

        // Сохраняем пост
        Post savedPost = postRepository.save(post);

        // Сохраняем изменения пользователя
        userRepository.save(currentUser);

        log.info("Пост успешно создан с ID: {}", savedPost.getId());

        return postMapper.toResponseDto(savedPost);
    }

    @Override
    public PostResponseDto getPostById(Long id) {
        log.info("Получение поста с ID: {}", id);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пост с ID " + id + " не найден"));

        // Увеличиваем счетчик просмотров
        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);

        log.info("Пост найден: {}", post.getContent().substring(0, Math.min(50, post.getContent().length())));

        return postMapper.toResponseDto(post);
    }

    @Override
    public PostListDto getUserPosts(Long userId, int page, int size) {
        log.info("Получение постов пользователя с ID: {}, страница: {}, размер: {}", userId, page, size);

        // Проверяем существование пользователя
        if (!userService.userExists(userId)) {
            throw new RuntimeException("Пользователь с ID " + userId + " не найден");
        }

        // Создаем Pageable для сортировки по дате создания (новые сначала)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Получаем посты пользователя (только неудаленные)
        Slice<Post> postSlice = postRepository.findByAuthor_IdAndIsDeletedFalse(userId, pageable);

        log.info("Найдено {} постов для пользователя {}", postSlice.getNumberOfElements(), userId);

        return postMapper.toListDtoFromSlice(postSlice);
    }

    /**
     * Получить посты текущего пользователя
     */
    @Override
    public PostListDto getMyPosts(int page, int size) {
        log.info("Получение собственных постов, страница: {}, размер: {}", page, size);
        User currentUser = getCurrentUser();
        return getUserPosts(currentUser.getId(), page, size);
    }

    @Override
    public PostListDto getFeedPosts(Long userId, int page, int size) {
        log.info("Получение ленты новостей для пользователя: {}, страница: {}, размер: {}", userId, page, size);

        // Получаем ID пользователей, на которых подписан текущий пользователь
        List<Long> subscriptionIds = subscriptionService.getSubscriptionIds(userId);

        if (subscriptionIds.isEmpty()) {
            log.info("У пользователя {} нет подписок, показываем рекомендованные посты", userId);
            return getRecommendedPosts(userId, page, size);
        }

        // Создаем Pageable с сортировкой по дате (новые сначала)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Получаем посты от пользователей из подписок
        Slice<Post> postSlice = postRepository.findByAuthor_IdInAndIsDeletedFalse(subscriptionIds, pageable);

        log.info("Найдено {} постов в ленте для пользователя {}", postSlice.getNumberOfElements(), userId);

        return postMapper.toListDtoFromSlice(postSlice);
    }

    @Override
    public PostListDto getRecommendedPosts(Long userId, int page, int size) {
        log.info("Получение рекомендованных постов для пользователя: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Показываем популярные посты (можно добавить другую логику)
        Slice<Post> postSlice = postRepository.findByIsDeletedFalseOrderByLikesCountDescCreatedAtDesc(pageable);

        return postMapper.toListDtoFromSlice(postSlice);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Post post = findById(postId);
        Long userId = post.getAuthor().getId();

        log.info("🗑️ Удаляем пост {} пользователя {}", postId, userId);

        // Логируем ДО удаления
        Long countBefore = postRepository.countByAuthorId(userId);
        log.info("📊 Количество постов ДО удаления: {}", countBefore);

        postRepository.deleteById(postId);

        // Логируем ПОСЛЕ удаления
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
