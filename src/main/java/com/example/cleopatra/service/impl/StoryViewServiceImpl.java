package com.example.cleopatra.service.impl;
import com.example.cleopatra.dto.StoryView.StoryViewDTO;
import com.example.cleopatra.maper.StoryViewManualMapper;
import com.example.cleopatra.model.Story;
import com.example.cleopatra.model.StoryView;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.StoryRepository;
import com.example.cleopatra.repository.StoryViewRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.StoryViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StoryViewServiceImpl implements StoryViewService {


    private final StoryViewRepository storyViewRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final StoryViewManualMapper mapper;
    private static final int DEFAULT_VIEWS_LIMIT = 100; // Лимит по умолчанию
    private static final int PREVIEW_VIEWS_LIMIT = 5;

    @Override
    public StoryViewDTO createView(Long storyId, Long viewerId) {
        log.info("Creating story view for storyId: {}, viewerId: {}", storyId, viewerId);

        // Проверяем, существует ли история
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("История не найдена с ID: " + storyId));

        // Проверяем, не истекла ли история
        if (story.isExpired()) {
            throw new IllegalStateException("История истекла и недоступна для просмотра");
        }

        // Проверяем, существует ли пользователь
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с ID: " + viewerId));

        // Проверяем, не просматривал ли уже пользователь эту историю
        if (hasUserViewedStory(storyId, viewerId)) {
            log.warn("Пользователь {} уже просматривал историю {}", viewerId, storyId);
            // Возвращаем существующий просмотр
            StoryView existingView = storyViewRepository.findByStoryIdAndViewerId(storyId, viewerId)
                    .orElseThrow(() -> new IllegalStateException("Ошибка получения существующего просмотра"));
            return mapper.toDTO(existingView);
        }

        // Создаем новый просмотр
        StoryView storyView = StoryView.builder()
                .story(story)
                .viewer(viewer)
                .viewedAt(LocalDateTime.now())
                .build();

        StoryView savedView = storyViewRepository.save(storyView);

        // Увеличиваем счетчик просмотров в истории
        story.incrementViewsCount();
        storyRepository.save(story);

        log.info("Story view created successfully with ID: {}", savedView.getId());
        return mapper.toDTO(savedView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryViewDTO> getStoryViews(Long storyId) {
        log.info("Getting story views for storyId: {}", storyId);

        // Проверяем, существует ли история
        if (!storyRepository.existsById(storyId)) {
            throw new IllegalArgumentException("История не найдена с ID: " + storyId);
        }

        // Получаем ограниченное количество просмотров с пользователями
        PageRequest pageRequest = PageRequest.of(0, DEFAULT_VIEWS_LIMIT, Sort.by("viewedAt").descending());
        Page<StoryView> viewsPage = storyViewRepository.findByStoryIdWithViewer(storyId, pageRequest);
        List<StoryView> views = viewsPage.getContent();

        log.info("Found {} views for story {} (limit: {})", views.size(), storyId, DEFAULT_VIEWS_LIMIT);
        return mapper.toDTO(views);
    }



    @Override
    @Transactional(readOnly = true)
    public boolean hasUserViewedStory(Long storyId, Long viewerId) {
        return storyViewRepository.existsByStoryIdAndViewerId(storyId, viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getViewsCount(Long storyId) {
        return storyViewRepository.countByStoryId(storyId);
    }
}