package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.WallPost.*;
import com.example.cleopatra.enums.WallAccessLevel;
import com.example.cleopatra.maper.WallPostMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.model.WallPost;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.repository.WallPostRepository;
import com.example.cleopatra.service.ImageValidator;
import com.example.cleopatra.service.StorageService;
import com.example.cleopatra.service.SubscriptionService;
import com.example.cleopatra.service.WallPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WallPostServiceImpl implements WallPostService {

    private final WallPostRepository wallPostRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final ImageValidator imageValidator;
    private final StorageService storageService;
    private final WallPostMapper wallPostMapper;

    @Override
    public WallPostCardResponse create(WallPostCreateRequest request, Long currentUserId) throws IOException {
        // 1. Валидация доступа к стене
        if (!canAccessWall(request.getWallOwnerId(), currentUserId)) {
            throw new AccessDeniedException("Нет доступа к записи на этой стене");
        }

        // 2. Получаем пользователей
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UsernameNotFoundException("Автор не найден"));
        User wallOwner = userRepository.findById(request.getWallOwnerId())
                .orElseThrow(() -> new UsernameNotFoundException("Владелец стены не найден"));

        String imageUrl = null;
        String imageId = null;

        try {
            // 3. Обработка изображения (если есть)
            if (request.getImage() != null && !request.getImage().isEmpty()) {
                var processedImage = imageValidator.validateAndProcess(request.getImage());
                var storageResult = storageService.uploadProcessedImage(processedImage);
                imageUrl = storageResult.getUrl();
                imageId = storageResult.getImageId();
            }

            WallPost wallPost = WallPost.builder()
                    .text(request.getText())
                    .picUrl(imageUrl)
                    .picId(imageId)
                    .author(author)
                    .wallOwner(wallOwner)
                    .likesCount(0L)
                    .commentsCount(0L)
                    .isEdited(false)
                    .build();

            WallPost savedPost = wallPostRepository.save(wallPost);
            log.info("Создан пост на стене: ID={}, автор={}, владелец стены={}",
                    savedPost.getId(), author.getId(), wallOwner.getId());

            return wallPostMapper.toCardResponse(savedPost, currentUserId);

        } catch (Exception e) {
            // 5. Откат загрузки изображения при ошибке
            if (imageId != null) {
                try {
                    storageService.deleteImage(imageId);
                    log.info("Откат загрузки изображения {} после ошибки", imageId);
                } catch (Exception deleteEx) {
                    log.error("Ошибка при откате загрузки изображения {}: {}",
                            imageId, deleteEx.getMessage());
                }
            }
            throw e;
        }
    }


    @Override
    @Transactional(readOnly = true)
    public WallPostPageResponse getWallPosts(Long wallOwnerId, Long currentUserId, int page, int size) {
        // 1. Проверяем доступ к стене
        if (!canAccessWall(wallOwnerId, currentUserId)) {
            throw new AccessDeniedException("Нет доступа к просмотру этой стены");
        }

        // 2. Создаем Pageable с сортировкой по дате создания (новые первые)
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // 3. Получаем посты стены
        Slice<WallPost> wallPostsSlice = wallPostRepository
                .findByWallOwnerId(wallOwnerId, pageable);

        // 4. Конвертируем в DTO
        var wallPostCards = wallPostsSlice.getContent()
                .stream()
                .map(post -> wallPostMapper.toCardResponse(post, currentUserId))
                .toList();

        return WallPostPageResponse.builder()
                .wallPosts(wallPostCards)
                .hasNext(wallPostsSlice.hasNext())
                .currentPage(page)
                .size(size)
                .isEmpty(wallPostCards.isEmpty())
                .numberOfElements(wallPostCards.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WallPostCardResponse getById(Long id, Long currentUserId) {
        WallPost wallPost = wallPostRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // Проверяем доступ к стене
        if (!canAccessWall(wallPost.getWallOwner().getId(), currentUserId)) {
            throw new AccessDeniedException("Нет доступа к просмотру этого поста");
        }

        return wallPostMapper.toCardResponse(wallPost, currentUserId);
    }

    // В сервисе изменить методы:
    @Override
    public void delete(Long postId, Long currentUserId) {
        WallPost wallPost = wallPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // Проверяем права на удаление
        if (!canDeletePost(wallPost, currentUserId)) {
            throw new AccessDeniedException("Нет прав на удаление этого поста");
        }

        wallPostRepository.delete(wallPost);


    }

    @Override
    public boolean canAccessWall(Long wallOwnerId, Long visitorId) {
        if (wallOwnerId.equals(visitorId)) {
            return true;
        }

        User wallOwner = userRepository.findById(wallOwnerId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        return switch (wallOwner.getWallAccessLevel()) {
            case PUBLIC -> true;
            // ✅ ИСПРАВЛЕНО: проверяем, подписан ли ВЛАДЕЛЕЦ стены на ПОСЕТИТЕЛЯ
            case FRIENDS -> subscriptionService.isSubscribed(wallOwnerId, visitorId);
            case PRIVATE -> false;
            case DISABLED -> false;
            default -> false;
        };
    }




    // Вспомогательные методы

    private boolean canDeletePost(WallPost wallPost, Long currentUserId) {
        // Может удалить автор поста или владелец стены
        return wallPost.getAuthor().getId().equals(currentUserId) ||
                wallPost.getWallOwner().getId().equals(currentUserId);
    }
}