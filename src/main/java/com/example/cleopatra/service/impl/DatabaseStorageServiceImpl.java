package com.example.cleopatra.service.impl;

import com.example.cleopatra.model.Story;
import com.example.cleopatra.repository.StoryRepository;
import com.example.cleopatra.service.DatabaseStorageService;
import com.example.cleopatra.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DatabaseStorageServiceImpl implements DatabaseStorageService {

    private final StoryRepository storyRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] getImageData(String imageId) {

        Story story = storyRepository.findByImageId(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Изображение не найдено: " + imageId));

        if (story.getImageData() == null) {
            throw new IllegalStateException("Данные изображения отсутствуют");
        }

        return story.getImageData();
    }

    @Override
    @Transactional(readOnly = true)
    public String getContentType(String imageId) {
        log.info("Getting content type for imageId: {}", imageId);

        Story story = storyRepository.findByImageId(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Изображение не найдено: " + imageId));

        return story.getContentType();
    }

    @Override
    public StorageService.StorageResult saveStoryImage(MultipartFile file) throws IOException {
        log.info("Saving story image to database, size: {} bytes", file.getSize());

        // Валидация файла
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        // Проверяем размер файла (например, максимум 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Файл слишком большой. Максимальный размер: 10MB");
        }

        // Проверяем тип файла
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Поддерживаются только изображения");
        }

        // Генерируем уникальный ID для изображения
        String imageId = UUID.randomUUID().toString();

        // Формируем URL для доступа к изображению
        String imageUrl = "/api/stories/image/" + imageId;

        log.info("Story image prepared with imageId: {}", imageId);

        // Возвращаем результат с imageId
        // imageData будет обработан в StoryService
        return new StorageService.StorageResult(imageUrl, imageId);
    }

    @Override
    public boolean deleteStoryImage(String imageId) {
        log.info("Deleting story image: {}", imageId);

        try {
            Story story = storyRepository.findByImageId(imageId).orElse(null);
            if (story == null) {
                log.warn("Story image not found: {}", imageId);
                return false;
            }

            // Удаляем историю (это удалит и изображение)
            storyRepository.delete(story);

            log.info("Story image deleted successfully: {}", imageId);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete story image: {}", imageId, e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getExpiredStoryImages() {
        log.info("Getting expired story images");

        LocalDateTime now = LocalDateTime.now();
        List<Story> expiredStories = storyRepository.findExpiredStories(now);

        return expiredStories.stream()
                .map(Story::getImageId)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteExpiredStories() {
        log.info("Deleting expired stories from database");

        LocalDateTime now = LocalDateTime.now();
        List<Story> expiredStories = storyRepository.findExpiredStories(now);

        int deletedCount = 0;
        for (Story story : expiredStories) {
            try {
                storyRepository.delete(story);
                deletedCount++;
            } catch (Exception e) {
                log.error("Failed to delete expired story: {}", story.getId(), e);
            }
        }

        log.info("Deleted {} expired stories from database", deletedCount);
        return deletedCount;
    }
}