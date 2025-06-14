package com.example.cleopatra.service.impl;
import com.example.cleopatra.dto.StoryDTO.StoryDTO;
import com.example.cleopatra.dto.StoryDTO.StoryList;
import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionCard;
import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionListDto;
import com.example.cleopatra.enums.StoryEmoji;
import com.example.cleopatra.maper.StoryManualMapper;
import com.example.cleopatra.model.Story;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.StoryRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.DatabaseStorageService;
import com.example.cleopatra.service.StorageService;
import com.example.cleopatra.service.StoryService;
import com.example.cleopatra.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final DatabaseStorageService databaseStorageService;
    private final StoryManualMapper storyMapper;
    private final SubscriptionService subscriptionService;


    @Override
    @Transactional(readOnly = true)
    public StoryList getSubscriptionsStories(Long currentUserId, Pageable pageable) {
        // Проверяем, существует ли пользователь
        if (!userRepository.existsById(currentUserId)) {
            throw new IllegalArgumentException("Пользователь не найден с ID: " + currentUserId);
        }

        try {
            // Получаем подписки пользователя
            UserSubscriptionListDto subscriptions = subscriptionService.getSubscriptions(currentUserId,
                    PageRequest.of(0, 1000)); // Берем большой лимит, чтобы получить все подписки

            if (subscriptions == null || subscriptions.getSubscriptions() == null || subscriptions.getSubscriptions().isEmpty()) {
                // Возвращаем пустой результат
                return createEmptyStoryList(pageable);
            }

            // Извлекаем ID пользователей, на которых подписан current user
            List<Long> subscribedUserIds = subscriptions.getSubscriptions()
                    .stream()
                    .map(UserSubscriptionCard::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (subscribedUserIds.isEmpty()) {
                log.debug("No valid subscription user IDs found for user {}", currentUserId);
                return createEmptyStoryList(pageable);
            }

            log.debug("Found {} subscriptions for user {}: {}", subscribedUserIds.size(), currentUserId, subscribedUserIds);

            // Получаем активные истории от пользователей из подписок
            LocalDateTime now = LocalDateTime.now();
            Page<Story> storiesPage = storyRepository.findActiveStoriesByUserIds(subscribedUserIds, now, pageable);

            // Преобразуем в DTO
            return storyMapper.toStoryList(storiesPage, currentUserId, false);

        } catch (Exception e) {
            log.error("Error getting subscription stories for user {}", currentUserId, e);
            // В случае ошибки возвращаем пустой результат вместо исключения
            return createEmptyStoryList(pageable);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public StoryList getSubscriptionsStories(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return getSubscriptionsStories(currentUserId, pageable);
    }

    /**
     * Создает пустой StoryList для случаев когда нет подписок или историй
     */
    private StoryList createEmptyStoryList(Pageable pageable) {
        StoryList emptyList = new StoryList();
        emptyList.setStories(Collections.emptyList());
        emptyList.setHasNext(false);
        emptyList.setHasPrevious(false);
        emptyList.setCurrentPage(pageable.getPageNumber());
        emptyList.setNextPage(null);
        emptyList.setPreviousPage(null);
        emptyList.setPageSize(pageable.getPageSize());
        emptyList.setIsEmpty(true);
        emptyList.setNumberOfElements(0);
        emptyList.setTotalElements(0L);
        emptyList.setTotalPages(0);
        return emptyList;
    }

    @Override
    public StoryDTO createStory(Long userId, MultipartFile file, StoryEmoji emoji, String description) throws IOException {
        System.out.println("НОВАЯ ВЕРСИЯ МЕТОДА!!!");
        log.info("=== СОЗДАНИЕ ИСТОРИИ - НАЧАЛО ===");

        // Проверяем пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с ID: " + userId));

        // Валидация файла
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл изображения обязателен");
        }

        // Проверяем тип файла
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new IllegalArgumentException("Поддерживаются только изображения");
        }

        // ДОБАВЬ ЭТО: конвертируй файл в byte[]
        byte[] imageData = file.getBytes();
        System.out.println("imageData size: " + imageData.length);

        System.out.println("imageData size: " + imageData.length);
        System.out.println("story.getImageData() size: " );
        // Сохраняем изображение через DatabaseStorageService
        StorageService.StorageResult storageResult = databaseStorageService.saveStoryImage(file);

        // Создаем Story entity
        Story story = Story.builder()
                .user(user)
                .imageId(storageResult.getImageId())
                .imageData(imageData)  // ← ДОБАВЬ ЭТУ СТРОКУ!
                .contentType(contentType)
                .emoji(emoji)
                .description(description)
                .viewsCount(0L)
                .build();

        System.out.println("story.getImageData() size: " +
                (story.getImageData() != null ? story.getImageData().length : "NULL"));

        // Сохраняем в БД
        Story savedStory = storyRepository.save(story);

        log.info("Story created successfully with ID: {}", savedStory.getId());
        return storyMapper.toDTO(savedStory, userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryDTO getStoryById(Long storyId, Long currentUserId) {

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("История не найдена с ID: " + storyId));

        // Проверяем, не истекла ли история
        if (story.isExpired()) {
            throw new IllegalStateException("История истекла и недоступна");
        }

        return storyMapper.toDTO(story, currentUserId, true); // включаем просмотры
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getStoryImage(String imageId) {
        // Проверяем, существует ли история с таким imageId
        Story story = storyRepository.findByImageId(imageId)
                .orElseThrow(() -> new IllegalArgumentException("История с изображением не найдена: " + imageId));

        // Проверяем, не истекла ли история
        if (story.isExpired()) {
            throw new IllegalStateException("История истекла, изображение недоступно");
        }

        return databaseStorageService.getImageData(imageId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getImageContentType(String imageId) {
        // Проверяем, существует ли история с таким imageId
        Story story = storyRepository.findByImageId(imageId)
                .orElseThrow(() -> new IllegalArgumentException("История с изображением не найдена: " + imageId));

        return databaseStorageService.getContentType(imageId);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryList getAllActiveStories(Pageable pageable, Long currentUserId) {
        log.info("Getting all active stories, page: {}", pageable.getPageNumber());

        LocalDateTime now = LocalDateTime.now();
        Page<Story> storiesPage = storyRepository.findAllActiveStories(now, pageable);

        return storyMapper.toStoryList(storiesPage, currentUserId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryList getAllActiveStories(int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return getAllActiveStories(pageable, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryList getUserStories(Long userId, Long currentUserId) {
        log.info("Getting stories for user: {}", userId);

        // Проверяем, существует ли пользователь
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Пользователь не найден с ID: " + userId);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Story> stories = storyRepository.findActiveStoriesByUserId(userId, now);

        List<StoryDTO> storyDTOs = storyMapper.toDTO(stories, currentUserId, false);

        // Создаем StoryList без пагинации
        StoryList result = new StoryList();
        result.setStories(storyDTOs);
        result.setIsEmpty(storyDTOs.isEmpty());
        result.setNumberOfElements(storyDTOs.size());
        result.setHasNext(false);
        result.setHasPrevious(false);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public StoryList getFollowingStories(Long userId, Pageable pageable) {
        log.info("Getting following stories for user: {}", userId);

        // Проверяем, существует ли пользователь
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Пользователь не найден с ID: " + userId);
        }

        LocalDateTime now = LocalDateTime.now();
        Page<Story> storiesPage = storyRepository.findFollowingActiveStories(userId, now, pageable);

        return storyMapper.toStoryList(storiesPage, userId, false);
    }

    @Override
    public StoryDTO updateStoryEmoji(Long storyId, Long userId, StoryEmoji emoji) {
        log.info("Updating story emoji: storyId={}, userId={}, emoji={}", storyId, userId, emoji);

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("История не найдена с ID: " + storyId));

        // Проверяем права доступа
        if (!story.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Нет прав для изменения этой истории");
        }

        // Проверяем, не истекла ли история
        if (story.isExpired()) {
            throw new IllegalStateException("Нельзя изменить истекшую историю");
        }

        story.setEmoji(emoji);
        Story updatedStory = storyRepository.save(story);

        log.info("Story emoji updated successfully");
        return storyMapper.toDTO(updatedStory, userId, false);
    }

    @Override
    public boolean deleteStory(Long storyId, Long userId) {
        log.info("Deleting story: storyId={}, userId={}", storyId, userId);

        Story story = storyRepository.findById(storyId).orElse(null);
        if (story == null) {
            log.warn("Story not found: {}", storyId);
            return false;
        }

        // Проверяем права доступа
        if (!story.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Нет прав для удаления этой истории");
        }

        // Удаляем изображение из хранилища
        try {
            databaseStorageService.deleteStoryImage(story.getImageId());
        } catch (Exception e) {
            log.warn("Failed to delete story image: {}", story.getImageId(), e);
        }

        // Удаляем историю (каскадно удалятся все просмотры)
        storyRepository.delete(story);

        log.info("Story deleted successfully");
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStoryActive(Long storyId) {
        return storyRepository.findById(storyId)
                .map(story -> !story.isExpired())
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUserActiveStoriesCount(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return storyRepository.countActiveStoriesByUserId(userId, now);
    }

    @Override
    public int deleteExpiredStories() {
        log.info("Deleting expired stories");

        LocalDateTime now = LocalDateTime.now();
        List<Story> expiredStories = storyRepository.findExpiredStories(now);

        int deletedCount = 0;
        for (Story story : expiredStories) {
            try {
                // Удаляем изображение
                databaseStorageService.deleteStoryImage(story.getImageId());

                // Удаляем историю
                storyRepository.delete(story);
                deletedCount++;

            } catch (Exception e) {
                log.error("Failed to delete expired story: {}", story.getId(), e);
            }
        }

        log.info("Deleted {} expired stories", deletedCount);
        return deletedCount;
    }

    @Override
    @Transactional
    public int deleteExpiredStoriesBatch(int batchSize) {
        log.debug("Deleting expired stories batch, size: {}", batchSize);

        LocalDateTime now = LocalDateTime.now();

        // Получаем только ID истекших историй с лимитом
        PageRequest pageRequest = PageRequest.of(0, batchSize);
        List<Long> expiredStoryIds = storyRepository.findExpiredStoryIds(now, pageRequest);

        if (expiredStoryIds.isEmpty()) {
            log.debug("No expired stories found in batch");
            return 0;
        }

        log.debug("Found {} expired stories in batch", expiredStoryIds.size());

        int deletedCount = 0;
        for (Long storyId : expiredStoryIds) {
            try {
                Story story = storyRepository.findById(storyId).orElse(null);
                if (story != null) {
                    // Удаляем изображение из хранилища
                    try {
                        databaseStorageService.deleteStoryImage(story.getImageId());
                    } catch (Exception e) {
                        log.warn("Failed to delete story image: {}", story.getImageId(), e);
                        // Продолжаем удаление истории даже если изображение не удалилось
                    }

                    // Удаляем историю (каскадно удалятся все просмотры)
                    storyRepository.delete(story);
                    deletedCount++;

                    log.debug("Deleted story with ID: {}", storyId);
                } else {
                    log.warn("Story with ID {} not found, skipping", storyId);
                }

            } catch (Exception e) {
                log.error("Failed to delete expired story in batch: {}", storyId, e);
                // Продолжаем обработку остальных записей
            }
        }

        log.debug("Batch completed: deleted {} out of {} stories", deletedCount, expiredStoryIds.size());
        return deletedCount;
    }

    @Override
    public long getTotalStoriesCount() {
        return storyRepository.count();
    }
}