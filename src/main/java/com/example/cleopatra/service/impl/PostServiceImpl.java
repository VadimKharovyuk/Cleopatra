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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final MentionService mentionService;



    @Override
    @Transactional
    @CacheEvict(value = {"user-posts", "post-stats", "recommended-posts"}, allEntries = true)
    public PostResponseDto createPost(PostCreateDto postCreateDto) {

        User currentUser = getCurrentUser();
        Post post = postMapper.toEntity(postCreateDto, currentUser);

        // ✅ ДОБАВЛЯЕМ логику для необязательной геолокации
        handleLocationForPost(post, postCreateDto);

        // Обработка изображения
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

        // Логирование локации перед сохранением
        if (post.getLocation() != null) {
            log.info("Location Coordinates: ({}, {})",
                    post.getLocation().getLatitude(), post.getLocation().getLongitude());

        }

        // ✅ ИСПРАВЛЕНИЕ: СНАЧАЛА сохраняем пост
        Post savedPost = postRepository.save(post);
        userRepository.save(currentUser);

        if (savedPost.getLocation() != null) {
            log.info("Saved Location Coordinates: ({}, {})",
                    savedPost.getLocation().getLatitude(), savedPost.getLocation().getLongitude());
        }
        try {
            mentionService.createPostMentions(savedPost); // ✅ Используем savedPost
        } catch (Exception e) {
            log.error("Ошибка при создании упоминаний для поста {}: {}", savedPost.getId(), e.getMessage());
        }

        // Получаем информацию о лайках
        Boolean isLiked = postLikeService.isPostLikedByUser(savedPost, currentUser.getId());
        List<PostResponseDto.LikeUserDto> recentLikes =
                postLikeService.getRecentLikes(savedPost, 5);

        // Создаем DTO для ответа
        PostResponseDto responseDto = postMapper.toResponseDto(savedPost, isLiked, recentLikes);
        if (responseDto.getLocation() != null) {
            log.info("Response Location Coordinates: ({}, {})",
                    responseDto.getLocation().getLatitude(), responseDto.getLocation().getLongitude());
        }

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

    }



    @Override
    @Cacheable(value = "posts", key = "#id")
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пост с ID " + id + " не найден"));

        // Увеличиваем счетчик просмотров
        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);

        Long commentsCount = commentService.getCommentsCount(post.getId());
        post.setCommentsCount(commentsCount);

        User currentUser = getCurrentUser();

        Boolean isLiked = postLikeService.isPostLikedByUser(post, currentUser.getId());
        List<PostResponseDto.LikeUserDto> recentLikes =
                postLikeService.getRecentLikes(post, 5);

        return postMapper.toResponseDto(post, isLiked, recentLikes);
    }




    @Override
    @Cacheable(value = "user-posts", key = "#userId + '_' + #page + '_' + #size")
    public PostListDto getUserPosts(Long userId, int page, int size) {

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
            Slice<Post> postSlice = postRepository.findByAuthor_IdAndIsDeletedFalse(userId, pageable);

            if (postSlice == null) {
                log.warn("Получен null от postRepository для пользователя {}", userId);
                postSlice = new SliceImpl<>(new ArrayList<>(), pageable, false);
            }

            log.info("Найдено {} постов пользователя {} на странице {}",
                    postSlice.getNumberOfElements(), userId, page);

            // Конвертируем в DTO с обработкой упоминаний
            PostListDto result = convertPostSliceToListDtoWithMentions(postSlice, page, userId);

            if (result == null) {
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
            return createEmptyPostListDto(page, size);
        }
    }

    // ✅ НОВЫЙ МЕТОД - конвертация с обработкой упоминаний
    private PostListDto convertPostSliceToListDtoWithMentions(Slice<Post> postSlice, int page, Long currentUserId) {
        try {
            List<PostCardDto> postCards = postSlice.getContent().stream()
                    .map(post -> {
                        try {
                            // Получаем информацию о лайках
                            Boolean isLiked = false;
                            List<PostCardDto.LikeUserDto> recentLikes = List.of();

                            // Если пользователь авторизован, получаем информацию о лайках
                            if (currentUserId != null) {
                                try {
                                    isLiked = postLikeService.isPostLikedByUser(post, currentUserId);

                                    // ✅ ИСПРАВЛЕНИЕ: Правильная конвертация типов
                                    recentLikes = postLikeService.getRecentLikes(post, 3)
                                            .stream()
                                            .map(likeUserDto -> PostCardDto.LikeUserDto.builder()
                                                    .id(likeUserDto.getId())
                                                    .firstName(likeUserDto.getFirstName())
                                                    .lastName(likeUserDto.getLastName())
                                                    .imageUrl(likeUserDto.getImageUrl())
                                                    .build())
                                            .collect(Collectors.toList());
                                } catch (Exception e) {
                                    log.warn("Ошибка получения информации о лайках для поста {}: {}",
                                            post.getId(), e.getMessage());
                                }
                            }

                            // ✅ ИСПОЛЬЗУЕМ toCardDto с обработкой упоминаний
                            return postMapper.toCardDto(post, isLiked, recentLikes);

                        } catch (Exception e) {
                            log.error("Ошибка конвертации поста {} в DTO: {}", post.getId(), e.getMessage());

                            // Fallback - возвращаем простую версию без упоминаний
                            return postMapper.toCardDtoSimple(post, false, List.of());
                        }
                    })
                    .filter(Objects::nonNull) // Исключаем null значения
                    .collect(Collectors.toList());

            return PostListDto.builder()
                    .posts(postCards)
                    .currentPage(page)
                    .hasNext(postSlice.hasNext())
                    .hasPrevious(postSlice.hasPrevious())
                    .nextPage(postSlice.hasNext() ? page + 1 : null)
                    .previousPage(page > 0 ? page - 1 : null)
                    .isEmpty(postCards.isEmpty())
                    .numberOfElements(postCards.size())
                    .build();

        } catch (Exception e) {
            log.error("Ошибка конвертации постов в DTO: {}", e.getMessage(), e);
            return null;
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
        User currentUser = getCurrentUser();
        return getUserPosts(currentUser.getId(), page, size);
    }

    @Override
    public PostListDto getFeedPosts(Long userId, int page, int size) {

        List<Long> subscriptionIds = subscriptionService.getSubscriptionIds(userId);

        if (subscriptionIds.isEmpty()) {
            return getRecommendedPosts(userId, page, size);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Slice<Post> postSlice = postRepository.findByAuthor_IdInAndIsDeletedFalse(subscriptionIds, pageable);
        // ✅ ОБНОВЛЕННЫЙ МЕТОД с логикой лайков
        return convertPostSliceToListDto(postSlice, page);
    }

    @Override
    public PostListDto getRecommendedPosts(Long userId, int page, int size) {

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

    @Override
    public long getTotalPostsCount() {
        return postRepository.count();
    }

    @Override
    public long getPostsCountByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return postRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }

    @Override
    public long getPostsCountFromDate(LocalDate fromDate) {
        LocalDateTime startDate = fromDate.atStartOfDay();
        return postRepository.countByCreatedAtGreaterThanEqual(startDate);
    }

    @Override
    public long getPostsCountBetweenDates(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return postRepository.countByCreatedAtBetween(start, end);
    }


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
    @CacheEvict(value = {"posts", "user-posts", "post-stats"}, allEntries = true)
    public void deletePost(Long postId) {
        Post post = findById(postId);
        Long userId = post.getAuthor().getId();

        log.info("🗑️ Удаляем пост {} пользователя {}", postId, userId);

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