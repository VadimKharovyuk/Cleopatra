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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final GroupPostMapper groupPostMapper;
    private final StorageService storageService;
    private final ImageValidator imageValidator;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "group-posts", allEntries = true),
            @CacheEvict(value = "group-stats", allEntries = true)
    })
    public GroupPostResponse createPost(CreateGroupPostRequest request, Long authorId) {
        log.info("🗑️ CACHE EVICT: Clearing group-posts and group-stats caches");
        validateCreatePostRequest(request, authorId);

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + request.getGroupId()));

        User author = userService.findById(authorId);
        GroupPost post = groupPostMapper.toEntity(request);

        // Устанавливаем связи
        post.setGroup(group);
        post.setAuthor(author);

        // Обрабатываем изображение если есть
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            try {
                ImageConverterService.ProcessedImage processedImage =
                        imageValidator.validateAndProcess(request.getImageUrl());

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

        GroupPost savedPost = groupPostRepository.save(post);
        groupRepository.incrementPostCount(group.getId());

        return groupPostMapper.toResponse(savedPost);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "group-posts", allEntries = true),
            @CacheEvict(value = "posts", key = "#postId")
    })
    public GroupPostResponse updatePost(Long postId, UpdateGroupPostRequest request, Long authorId) {
        log.debug("Updating post {} by user {}", postId, authorId);

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (!post.getAuthorId().equals(authorId)) {
            throw new RuntimeException("User is not authorized to edit this post");
        }

        String oldImgId = post.getImgId();
        boolean imageChanged = false;

        // Обновляем текст
        if (request.getText() != null) {
            post.setText(request.getText());
        }

        // Обрабатываем изображение
        if (request.getImageUrl() != null) {
            if (!request.getImageUrl().isEmpty()) {
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
            }
        }

        log.info("Successfully updated post {}", postId);
        return groupPostMapper.toResponse(updatedPost);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "posts", key = "#postId")
    public GroupPostResponse getPostById(Long postId) {
        log.debug("Getting post by id: {}", postId);

        if (postId == null) {
            throw new IllegalArgumentException("Post ID cannot be null");
        }

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        log.debug("Found post: {} in group: {} by author: {}",
                postId, post.getGroupId(), post.getAuthorId());

        return groupPostMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "group-posts", key = "#groupId + '_' + #page + '_' + #size + '_' + (#currentUserId != null ? #currentUserId : 'anonymous')")
    public GroupPostsSliceResponse getGroupPosts(Long groupId, Long currentUserId, int page, int size) {
        String cacheKey = groupId + "_" + page + "_" + size + "_" + (currentUserId != null ? currentUserId : "anonymous");
        log.info("🔍 CACHE MISS: Loading group posts for key: {}", cacheKey);

        validateGetPostsRequest(groupId, page, size);

        if (!groupRepository.existsById(groupId)) {
            throw new RuntimeException("Group not found with id: " + groupId);
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Slice<GroupPostDetails> slice;

        if (currentUserId != null) {
            slice = groupPostRepository.findGroupPostsByGroupIdWithLikes(groupId, currentUserId, pageRequest);
        } else {
            slice = groupPostRepository.findGroupPostsByGroupId(groupId, pageRequest);
        }

        return groupPostMapper.fromDetailsSlice(slice);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "group-posts", allEntries = true),
            @CacheEvict(value = "group-stats", allEntries = true),
            @CacheEvict(value = "posts", key = "#postId")
    })
    public void deletePost(Long postId, Long authorId) {
        log.debug("Deleting post {} by user {}", postId, authorId);

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (!post.getAuthorId().equals(authorId)) {
            throw new RuntimeException("User is not authorized to delete this post");
        }

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
            }
        }

        groupRepository.decrementPostCount(post.getGroupId());
    }

    // Существующие приватные методы остаются без изменений
    private void validateCreatePostRequest(CreateGroupPostRequest request, Long authorId) {
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }

        if (request.getGroupId() == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        boolean hasText = request.getText() != null && !request.getText().trim().isEmpty();
        boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().isEmpty();

        if (!hasText && !hasImage) {
            throw new IllegalArgumentException("Post must contain either text or image");
        }

        if (hasText && request.getText().trim().length() > 5000) {
            throw new IllegalArgumentException("Text cannot exceed 5000 characters");
        }
    }

    private void validateGetPostsRequest(Long groupId, int page, int size) {
        if (groupId == null) {
            log.warn("Attempt to get posts with null groupId");
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        if (groupId <= 0) {
            log.warn("Attempt to get posts with invalid groupId: {}", groupId);
            throw new IllegalArgumentException("Group ID must be positive");
        }

        if (page < 0) {
            log.warn("Attempt to get posts with negative page number: {}", page);
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (size <= 0) {
            log.warn("Attempt to get posts with non-positive size: {}", size);
            throw new IllegalArgumentException("Page size must be positive");
        }

        if (size > 100) {
            log.warn("Attempt to get posts with too large page size: {}", size);
            throw new IllegalArgumentException("Page size cannot exceed 100 items per page");
        }

        log.debug("Get posts request validation passed: groupId={}, page={}, size={}",
                groupId, page, size);
    }
}
