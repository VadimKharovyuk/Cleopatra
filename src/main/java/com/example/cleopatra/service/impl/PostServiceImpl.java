package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.PostNotFoundException;
import com.example.cleopatra.dto.Post.*;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.maper.PostMapper;
import com.example.cleopatra.model.Location;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.CommentRepository;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final LocationService locationService;



    @Override
    public PostResponseDto createPost(PostCreateDto postCreateDto) {
        log.info("=== НАЧАЛО СОЗДАНИЯ ПОСТА ===");
        log.info("Содержимое поста: {}", postCreateDto.getContent());

        // ✅ ДОБАВЛЯЕМ детальные логи для геолокации
        log.info("=== ПРОВЕРКА ГЕОЛОКАЦИИ ===");
        log.info("LocationId: {}", postCreateDto.getLocationId());
        log.info("Latitude: {}", postCreateDto.getLatitude());
        log.info("Longitude: {}", postCreateDto.getLongitude());
        log.info("PlaceName: {}", postCreateDto.getPlaceName());

        User currentUser = getCurrentUser();
        Post post = postMapper.toEntity(postCreateDto, currentUser);

        log.info("Пост создан в маппере, ID: {}", post.getId());

        // ✅ ДОБАВЛЯЕМ логику для необязательной геолокации
        handleLocationForPost(post, postCreateDto);

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

        // ✅ ЛОГИРУЕМ СОСТОЯНИЕ ПОСТА ПЕРЕД СОХРАНЕНИЕМ
        log.info("=== СОСТОЯНИЕ ПОСТА ПЕРЕД СОХРАНЕНИЕМ ===");
        log.info("Post ID: {}", post.getId());
        log.info("Post Content: {}", post.getContent());
        log.info("Post Author: {}", post.getAuthor().getFirstName());
        log.info("Post Location: {}", post.getLocation());
        if (post.getLocation() != null) {
            log.info("Location ID: {}", post.getLocation().getId());
            log.info("Location Coordinates: ({}, {})",
                    post.getLocation().getLatitude(), post.getLocation().getLongitude());
            log.info("Location PlaceName: {}", post.getLocation().getPlaceName());
        }

        Post savedPost = postRepository.save(post);
        userRepository.save(currentUser);

        // ✅ ЛОГИРУЕМ СОСТОЯНИЕ ПОСЛЕ СОХРАНЕНИЯ
        log.info("=== СОСТОЯНИЕ ПОСЛЕ СОХРАНЕНИЯ ===");
        log.info("Saved Post ID: {}", savedPost.getId());
        log.info("Saved Post Location: {}", savedPost.getLocation());
        if (savedPost.getLocation() != null) {
            log.info("Saved Location ID: {}", savedPost.getLocation().getId());
            log.info("Saved Location Coordinates: ({}, {})",
                    savedPost.getLocation().getLatitude(), savedPost.getLocation().getLongitude());
        }

        log.info("Пост успешно создан с ID: {}", savedPost.getId());

        // ✅ ОБНОВЛЕННЫЙ ВЫЗОВ с логикой лайков
        Boolean isLiked = postLikeService.isPostLikedByUser(savedPost, currentUser.getId());
        List<PostResponseDto.LikeUserDto> recentLikes =
                postLikeService.getRecentLikes(savedPost, 5);

        PostResponseDto responseDto = postMapper.toResponseDto(savedPost, isLiked, recentLikes);

        // ✅ ЛОГИРУЕМ ФИНАЛЬНЫЙ DTO
        log.info("=== ФИНАЛЬНЫЙ RESPONSE DTO ===");
        log.info("Response DTO Location: {}", responseDto.getLocation());
        if (responseDto.getLocation() != null) {
            log.info("Response Location ID: {}", responseDto.getLocation().getId());
            log.info("Response Location Coordinates: ({}, {})",
                    responseDto.getLocation().getLatitude(), responseDto.getLocation().getLongitude());
        }
        log.info("=== КОНЕЦ СОЗДАНИЯ ПОСТА ===");

        return responseDto;
    }

    // ✅ ДОБАВЛЯЕМ приватный метод для обработки локации с подробными логами
    private void handleLocationForPost(Post post, PostCreateDto postCreateDto) {
        log.info("=== ОБРАБОТКА ГЕОЛОКАЦИИ ===");

        try {
            // Вариант 1: Используем существующую локацию по ID
            if (postCreateDto.getLocationId() != null) {
                log.info("Используем существующую локацию с ID: {}", postCreateDto.getLocationId());
                Location location = locationService.findById(postCreateDto.getLocationId());
                post.setLocation(location);
                log.info("К посту добавлена существующая локация с ID: {} (координаты: {}, {})",
                        postCreateDto.getLocationId(), location.getLatitude(), location.getLongitude());
            }
            // Вариант 2: Создаем новую локацию из координат
            else if (postCreateDto.getLatitude() != null && postCreateDto.getLongitude() != null) {
                log.info("Создаем новую локацию из координат: ({}, {}) с названием: {}",
                        postCreateDto.getLatitude(), postCreateDto.getLongitude(), postCreateDto.getPlaceName());

                Location location = locationService.createLocationFromCoordinates(
                        postCreateDto.getLatitude(),
                        postCreateDto.getLongitude(),
                        postCreateDto.getPlaceName()
                );

                log.info("Локация создана с ID: {}", location.getId());
                post.setLocation(location);
                log.info("К посту добавлена новая локация: {} (ID: {}, координаты: {}, {})",
                        location.getPlaceName(), location.getId(), location.getLatitude(), location.getLongitude());
            }
            // Вариант 3: Без локации (по умолчанию)
            else {
                post.setLocation(null);
                log.info("Пост создается без геолокации - все поля локации пустые");
            }

            log.info("Финальное состояние локации в посте: {}", post.getLocation());

        } catch (Exception e) {
            log.error("Ошибка при обработке геолокации для поста: {}", e.getMessage(), e);
            post.setLocation(null);
            log.warn("Локация установлена в null из-за ошибки");
        }

        log.info("=== КОНЕЦ ОБРАБОТКИ ГЕОЛОКАЦИИ ===");
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
        log.info("Получение постов пользователя: {}, страница: {}, размер: {}", userId, page, size);

        try {
            // Проверяем валидность параметров
            if (userId == null) {
                log.error("userId не может быть null");
                return createEmptyPostListDto(page, size);
            }

            if (!userService.userExists(userId)) {
                log.error("Пользователь с ID {} не найден", userId);
                throw new RuntimeException("Пользователь с ID " + userId + " не найден");
            }

            if (page < 0) {
                log.warn("Отрицательный номер страницы: {}, устанавливаем 0", page);
                page = 0;
            }

            if (size <= 0 || size > 20) {
                log.warn("Некорректный размер страницы: {}, устанавливаем 6", size);
                size = 6;
            }

            // Создаем Pageable
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            // Получаем посты пользователя
            Slice<Post> postSlice = null;
            try {
                postSlice = postRepository.findByAuthor_IdAndIsDeletedFalse(userId, pageable);
            } catch (Exception e) {
                log.error("Ошибка запроса к БД для постов пользователя {}: {}", userId, e.getMessage());
                return createEmptyPostListDto(page, size);
            }

            if (postSlice == null) {
                log.warn("Получен null от postRepository для пользователя {}", userId);
                postSlice = new SliceImpl<>(new ArrayList<>(), pageable, false);
            }

            log.info("Найдено {} постов пользователя {} на странице {}",
                    postSlice.getNumberOfElements(), userId, page);

            // Конвертируем в DTO
            PostListDto result = convertPostSliceToListDto(postSlice, page);

            if (result == null) {
                log.warn("convertPostSliceToListDto вернул null для пользователя {}", userId);
                result = createEmptyPostListDto(page, size);
            }

            if (result.getPosts() == null) {
                result.setPosts(new ArrayList<>());
            }

            // Дополняем информацию
            result.setPageSize(size);
            result.setIsEmpty(result.getPosts().isEmpty());
            result.setNumberOfElements(result.getPosts().size());

            return result;

        } catch (Exception e) {
            log.error("Неожиданная ошибка в getUserPosts для пользователя {}: {}", userId, e.getMessage(), e);

            // Если это наше исключение (пользователь не найден), пробрасываем его
            if (e.getMessage().contains("не найден")) {
                throw e;
            }

            // Для других ошибок возвращаем пустой результат
            return createEmptyPostListDto(page, size);
        }
    }

    // Вспомогательный метод для создания пустого PostListDto
    private PostListDto createEmptyPostListDto(int page, int size) {
        return PostListDto.builder()
                .posts(new ArrayList<>())
                .hasNext(false)
                .hasPrevious(page > 0)
                .currentPage(page)
                .nextPage(null)
                .previousPage(page > 0 ? page - 1 : null)
                .pageSize(size)
                .isEmpty(true)
                .numberOfElements(0)
                .build();
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