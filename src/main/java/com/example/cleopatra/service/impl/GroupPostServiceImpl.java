package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.GroupDto.*;
import com.example.cleopatra.maper.GroupPostMapper;
import com.example.cleopatra.model.Group;
import com.example.cleopatra.model.GroupPost;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.GroupPostRepository;
import com.example.cleopatra.repository.GroupRepository;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupPostServiceImpl implements GroupPostService {


    private final GroupPostRepository groupPostRepository;
    private final GroupRepository groupRepository;
    private final UserService userService;
    private final GroupPostMapper groupPostMapper ;
    private final StorageService storageService;
    private final ImageValidator imageValidator ;


    @Override
    @Transactional
    public GroupPostResponse createPost(CreateGroupPostRequest request, Long authorId) {
        log.debug("Creating post for group {} by user {}", request.getGroupId(), authorId);

        // 1. Валидация входных данных
        validateCreatePostRequest(request, authorId);

        // 2. Проверяем существование группы
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + request.getGroupId()));

        // 3. Получаем пользователя через UserService
        User author = userService.findById(authorId);

        // 4. Создаем пост через маппер
        GroupPost post = groupPostMapper.toEntity(request);

        // 5. Устанавливаем связи
        post.setGroup(group);
        post.setAuthor(author);

        // 6. Обрабатываем изображение если есть
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            try {
                // Валидируем и обрабатываем изображение
                ImageConverterService.ProcessedImage processedImage =
                        imageValidator.validateAndProcess(request.getImageUrl());

                // Загружаем в хранилище
                StorageService.StorageResult storageResult =
                        storageService.uploadProcessedImage(processedImage);

                post.setImageUrl(storageResult.getUrl());
                post.setImgId(storageResult.getImageId());

                log.info("Image uploaded successfully for post. URL: {}, ID: {}",
                        storageResult.getUrl(), storageResult.getImageId());

            } catch (Exception e) {
                log.error("Failed to upload image for post: {}", e.getMessage());
                throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
            }
        }

        // 7. Сохраняем пост
        GroupPost savedPost = groupPostRepository.save(post);

        // 8. Обновляем счетчик постов в группе
        groupRepository.incrementPostCount(group.getId());

        log.info("Created post with id {} for group {} by user {}",
                savedPost.getId(), group.getId(), authorId);

        // 9. Возвращаем DTO через маппер
        return groupPostMapper.toResponse(savedPost);
    }

    /**
     * Валидирует запрос на создание поста
     */
    private void validateCreatePostRequest(CreateGroupPostRequest request, Long authorId) {
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }

        if (request.getGroupId() == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        // Проверяем, что есть либо текст, либо изображение
        boolean hasText = request.getText() != null && !request.getText().trim().isEmpty();
        boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().isEmpty();

        if (!hasText && !hasImage) {
            throw new IllegalArgumentException("Post must contain either text or image");
        }

        // Дополнительная валидация текста
        if (hasText && request.getText().trim().length() > 5000) {
            throw new IllegalArgumentException("Text cannot exceed 5000 characters");
        }
    }


    @Override
    @Transactional
    public GroupPostResponse updatePost(Long postId, UpdateGroupPostRequest request, Long authorId) {
        log.debug("Updating post {} by user {}", postId, authorId);

        // Находим пост
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // Проверяем права на редактирование
        if (!post.getAuthorId().equals(authorId)) {
            throw new RuntimeException("User is not authorized to edit this post");
        }

        // Сохраняем старые данные изображения для возможного удаления
        String oldImgId = post.getImgId();
        boolean imageChanged = false;

        // Обновляем текст
        if (request.getText() != null) {
            post.setText(request.getText());
        }

        // Обрабатываем изображение
        if (request.getImageUrl() != null) {
            if (!request.getImageUrl().isEmpty()) {
                // Загружаем новое изображение
                try {
                    ImageConverterService.ProcessedImage processedImage =
                            imageValidator.validateAndProcess(request.getImageUrl());

                    StorageService.StorageResult storageResult =
                            storageService.uploadProcessedImage(processedImage);

                    post.setImageUrl(storageResult.getUrl());
                    post.setImgId(storageResult.getImageId());
                    imageChanged = true;

                    log.info("Updated image for post {}. New URL: {}, ID: {}",
                            postId, storageResult.getUrl(), storageResult.getImageId());

                } catch (Exception e) {
                    log.error("Failed to upload new image for post {}: {}", postId, e.getMessage());
                    throw new RuntimeException("Failed to upload new image: " + e.getMessage(), e);
                }
            } else {
                post.setImageUrl(null);
                post.setImgId(null);
                imageChanged = true;

                log.info("Removed image from post {}", postId);
            }
        }

        // Сохраняем обновленный пост
        GroupPost updatedPost = groupPostRepository.save(post);

        // Удаляем старое изображение после успешного сохранения
        if (imageChanged && oldImgId != null) {
            try {
                boolean deleted = storageService.deleteImage(oldImgId);
                if (deleted) {
                    log.info("Successfully deleted old image {} for post {}", oldImgId, postId);
                } else {
                    log.warn("Failed to delete old image {} for post {}", oldImgId, postId);
                }
            } catch (Exception e) {
                log.error("Error deleting old image {} for post {}: {}", oldImgId, postId, e.getMessage());
                // Не прерываем выполнение, так как пост уже обновлен
            }
        }

        log.info("Successfully updated post {}", postId);

        return groupPostMapper.toResponse(updatedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupPostResponse getPostById(Long postId) {
        log.debug("Getting post by id: {}", postId);

        if (postId == null) {
            throw new IllegalArgumentException("Post ID cannot be null");
        }

        // Находим пост с загрузкой связанных сущностей
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));


        log.debug("Found post: {} in group: {} by author: {}",
                postId, post.getGroupId(), post.getAuthorId());

        // Преобразуем в DTO через маппер
        GroupPostResponse response = groupPostMapper.toResponse(post);

        log.debug("Successfully retrieved post {}", postId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public GroupPostsSliceResponse getGroupPosts(Long groupId, Long currentUserId, int page, int size) {
        log.debug("Getting posts for group: {}, page: {}, size: {}, currentUser: {}",
                groupId, page, size, currentUserId);

        // Валидация входных параметров
        validateGetPostsRequest(groupId, page, size);

        // Проверяем существование группы
        if (!groupRepository.existsById(groupId)) {
            throw new RuntimeException("Group not found with id: " + groupId);
        }

        // Создаем объект пагинации
        PageRequest pageRequest = PageRequest.of(page, size);

        // Получаем данные через Repository (используем projection)
        Slice<GroupPostDetails> slice;

        if (currentUserId != null) {
            log.debug("Fetching posts with user-specific data for user: {}", currentUserId);
            slice = groupPostRepository.findGroupPostsByGroupIdWithLikes(groupId, currentUserId, pageRequest);
        } else {
            log.debug("Fetching posts without user-specific data");
            slice = groupPostRepository.findGroupPostsByGroupId(groupId, pageRequest);
        }

        // Логируем результат
        log.debug("Found {} posts for group {}, hasNext: {}",
                slice.getNumberOfElements(), groupId, slice.hasNext());

        // ИСПОЛЬЗУЕМ МАППЕР: метод fromDetailsSlice()
        GroupPostsSliceResponse response = groupPostMapper.fromDetailsSlice(slice);

        log.debug("Successfully retrieved posts for group {}", groupId);
        return response;
    }


    private void validateGetPostsRequest(Long groupId, int page, int size) {
        // Проверяем groupId
        if (groupId == null) {
            log.warn("Attempt to get posts with null groupId");
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        if (groupId <= 0) {
            log.warn("Attempt to get posts with invalid groupId: {}", groupId);
            throw new IllegalArgumentException("Group ID must be positive");
        }

        // Проверяем page
        if (page < 0) {
            log.warn("Attempt to get posts with negative page number: {}", page);
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        // Проверяем size
        if (size <= 0) {
            log.warn("Attempt to get posts with non-positive size: {}", size);
            throw new IllegalArgumentException("Page size must be positive");
        }

        // Ограничиваем максимальный размер страницы для защиты от перегрузки
        if (size > 100) {
            log.warn("Attempt to get posts with too large page size: {}", size);
            throw new IllegalArgumentException("Page size cannot exceed 100 items per page");
        }

        log.debug("Get posts request validation passed: groupId={}, page={}, size={}",
                groupId, page, size);
    }

    /**
     * Переопределенный метод удаления с очисткой изображений
     */
    @Override
    @Transactional
    public void deletePost(Long postId, Long authorId) {
        log.debug("Deleting post {} by user {}", postId, authorId);

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // Проверяем права на удаление
        if (!post.getAuthorId().equals(authorId)) {
            throw new RuntimeException("User is not authorized to delete this post");
        }

        // Сохраняем ID изображения для удаления
        String imgId = post.getImgId();

        // Удаляем пост из базы данных
        groupPostRepository.delete(post);

        // Удаляем изображение из хранилища
        if (imgId != null) {
            try {
                boolean deleted = storageService.deleteImage(imgId);
                if (deleted) {
                    log.info("Successfully deleted image {} for post {}", imgId, postId);
                } else {
                    log.warn("Failed to delete image {} for post {}", imgId, postId);
                }
            } catch (Exception e) {
                log.error("Error deleting image {} for post {}: {}", imgId, postId, e.getMessage());
                // Не прерываем выполнение, так как пост уже удален
            }
        }

         groupRepository.decrementPostCount(post.getGroupId());

        log.info("Successfully deleted post {} by user {}", postId, authorId);
    }
}
