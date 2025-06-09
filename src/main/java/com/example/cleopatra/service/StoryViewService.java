package com.example.cleopatra.service;


import com.example.cleopatra.dto.StoryView.StoryViewDTO;

import java.util.List;

public interface StoryViewService {

    /**
     * Создать просмотр истории
     * @param storyId ID истории
     * @param viewerId ID пользователя, который просматривает
     * @return DTO созданного просмотра
     */
    StoryViewDTO createView(Long storyId, Long viewerId);

    /**
     * Получить список всех просмотров истории
     * @param storyId ID истории
     * @return список просмотров (последние 100)
     */
    List<StoryViewDTO> getStoryViews(Long storyId);

    /**
     * Проверить, просматривал ли пользователь историю
     * @param storyId ID истории
     * @param viewerId ID пользователя
     * @return true если уже просматривал
     */
    boolean hasUserViewedStory(Long storyId, Long viewerId);

    /**
     * Получить количество просмотров истории
     * @param storyId ID истории
     * @return количество просмотров
     */
    Long getViewsCount(Long storyId);
}