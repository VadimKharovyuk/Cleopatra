package com.example.cleopatra.service;

import com.example.cleopatra.dto.StoryDTO.StoryDTO;
import com.example.cleopatra.dto.StoryDTO.StoryList;
import com.example.cleopatra.enums.StoryEmoji;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StoryService {

    /**
     * Получить ленту историй подписок текущего пользователя
     * @param currentUserId ID текущего пользователя
     * @param pageable параметры пагинации
     * @return истории людей, на которых подписан пользователь
     */
    StoryList getSubscriptionsStories(Long currentUserId, Pageable pageable);

    /**
     * Удобный метод для ленты историй
     * @param currentUserId ID текущего пользователя
     * @param page номер страницы
     * @param size размер страницы
     * @return истории подписок
     */
    StoryList getSubscriptionsStories(Long currentUserId, int page, int size);
    /**
     * Создать новую историю
     * @param userId ID пользователя
     * @param file изображение для истории
     * @param emoji эмодзи (опционально)
     * @param description описание (опционально)
     * @return DTO созданной истории
     */
    StoryDTO createStory(Long userId, MultipartFile file, StoryEmoji emoji, String description) throws IOException;

    /**
     * Получить историю по ID
     * @param storyId ID истории
     * @param currentUserId ID текущего пользователя (для контекста)
     * @return DTO истории
     */
    StoryDTO getStoryById(Long storyId, Long currentUserId);

    /**
     * Получить изображение истории
     * @param imageId ID изображения
     * @return массив байтов изображения
     */
    byte[] getStoryImage(String imageId);

    /**
     * Получить тип контента изображения
     * @param imageId ID изображения
     * @return MIME тип (image/jpeg, image/png)
     */
    String getImageContentType(String imageId);

    /**
     * Получить все активные истории с пагинацией
     * @param pageable параметры пагинации
     * @param currentUserId ID текущего пользователя
     * @return список историй с пагинацией
     */
    StoryList getAllActiveStories(Pageable pageable, Long currentUserId);

    /**
     * Получить все активные истории с пагинацией (удобный метод)
     * @param page номер страницы
     * @param size размер страницы
     * @param currentUserId ID текущего пользователя
     * @return список историй с пагинацией
     */
    StoryList getAllActiveStories(int page, int size, Long currentUserId);

    /**
     * Получить истории конкретного пользователя
     * @param userId ID пользователя
     * @param currentUserId ID текущего пользователя (для контекста)
     * @return список историй пользователя
     */
    StoryList getUserStories(Long userId, Long currentUserId);

    /**
     * Получить истории подписок пользователя (лента)
     * @param userId ID пользователя
     * @param pageable параметры пагинации
     * @return истории из ленты
     */
    StoryList getFollowingStories(Long userId, Pageable pageable);


    /**
     * Обновить эмодзи истории
     * @param storyId ID истории
     * @param userId ID пользователя (только владелец может изменять)
     * @param emoji новое эмодзи
     * @return обновленная история
     */
    StoryDTO updateStoryEmoji(Long storyId, Long userId, StoryEmoji emoji);

    /**
     * Удалить историю
     * @param storyId ID истории
     * @param userId ID пользователя (только владелец может удалять)
     * @return true если удалена успешно
     */
    boolean deleteStory(Long storyId, Long userId);

    /**
     * Проверить, существует ли история и не истекла ли она
     * @param storyId ID истории
     * @return true если история активна
     */
    boolean isStoryActive(Long storyId);

    /**
     * Получить количество активных историй пользователя
     * @param userId ID пользователя
     * @return количество активных историй
     */
    Long getUserActiveStoriesCount(Long userId);

    /**
     * Удалить все истекшие истории (для шедулера)
     * @return количество удаленных историй
     */
    int deleteExpiredStories();


    int deleteExpiredStoriesBatch(int batchSize);


    long getTotalStoriesCount();
}