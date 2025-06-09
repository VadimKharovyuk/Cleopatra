package com.example.cleopatra.maper;

import com.example.cleopatra.dto.StoryDTO.StoryDTO;
import com.example.cleopatra.dto.StoryDTO.StoryList;
import com.example.cleopatra.dto.StoryView.StoryViewDTO;
import com.example.cleopatra.maper.StoryViewManualMapper;
import com.example.cleopatra.model.Story;
import com.example.cleopatra.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StoryManualMapper {

    @Autowired
    private StoryViewManualMapper storyViewMapper;

    // ===== Основные методы маппинга =====

    public StoryDTO toDTO(Story story) {
        return toDTO(story, null, false);
    }

    public StoryDTO toDTO(Story story, Long currentUserId) {
        return toDTO(story, currentUserId, false);
    }

    public StoryDTO toDTO(Story story, Long currentUserId, boolean includeRecentViews) {
        if (story == null) {
            return null;
        }

        StoryDTO dto = new StoryDTO();
        dto.setId(story.getId());

        // Маппинг данных пользователя с проверками на null
        User user = story.getUser();
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setUserFirstName(user.getFirstName());
            dto.setUserLastName(user.getLastName());
            dto.setUserImageUrl(user.getImageUrl());
        }

        // Данные изображения (без byte[])
        dto.setImageId(story.getImageId());
        dto.setImageUrl("/api/stories/image/" + story.getImageId()); // URL для получения изображения
        dto.setContentType(story.getContentType());

        // Контент
        dto.setEmoji(story.getEmoji());
        dto.setDescription(story.getDescription());

        // Время
        dto.setCreatedAt(story.getCreatedAt());
        dto.setExpiresAt(story.getExpiresAt());
        dto.setTimeAgo(calculateTimeAgo(story.getCreatedAt()));
        dto.setExpiresIn(calculateExpiresIn(story.getExpiresAt()));

        // Статистика
        dto.setViewsCount(story.getViewsCount());

        // Последние просмотры (если нужно)
        if (includeRecentViews && story.getViews() != null && !story.getViews().isEmpty()) {
            List<StoryViewDTO> recentViews = story.getViews().stream()
                    .filter(Objects::nonNull)
                    .limit(5) // Последние 5 просмотров
                    .map(storyViewMapper::toDTO)
                    .collect(Collectors.toList());
            dto.setRecentViews(recentViews);
        } else {
            dto.setRecentViews(Collections.emptyList());
        }

        // Статус для текущего пользователя
        if (currentUserId != null) {
            dto.setIsOwner(user != null && Objects.equals(user.getId(), currentUserId));
            dto.setIsViewedByCurrentUser(hasUserViewed(story, currentUserId));
        } else {
            dto.setIsOwner(false);
            dto.setIsViewedByCurrentUser(false);
        }

        dto.setIsExpired(story.isExpired());

        return dto;
    }

    // ===== Методы для списков =====

    public List<StoryDTO> toDTO(List<Story> stories) {
        return toDTO(stories, null, false);
    }

    public List<StoryDTO> toDTO(List<Story> stories, Long currentUserId) {
        return toDTO(stories, currentUserId, false);
    }

    public List<StoryDTO> toDTO(List<Story> stories, Long currentUserId, boolean includeRecentViews) {
        if (stories == null || stories.isEmpty()) {
            return Collections.emptyList();
        }

        return stories.stream()
                .filter(Objects::nonNull)
                .map(story -> toDTO(story, currentUserId, includeRecentViews))
                .collect(Collectors.toList());
    }

    // ===== Методы для пагинации =====

    public StoryList toStoryList(Page<Story> page) {
        return toStoryList(page, null, false);
    }

    public StoryList toStoryList(Page<Story> page, Long currentUserId) {
        return toStoryList(page, currentUserId, false);
    }

    public StoryList toStoryList(Page<Story> page, Long currentUserId, boolean includeRecentViews) {
        if (page == null) {
            return new StoryList();
        }

        // Маппим Story в StoryDTO
        List<StoryDTO> storyDTOs = toDTO(page.getContent(), currentUserId, includeRecentViews);

        // Создаем Page<StoryDTO> для использования в StoryList.fromPage()
        Page<StoryDTO> dtoPage = new org.springframework.data.domain.PageImpl<>(
                storyDTOs,
                page.getPageable(),
                page.getTotalElements()
        );

        return StoryList.fromPage(dtoPage);
    }

    public StoryList toStoryList(Slice<Story> slice) {
        return toStoryList(slice, null, false);
    }

    public StoryList toStoryList(Slice<Story> slice, Long currentUserId) {
        return toStoryList(slice, currentUserId, false);
    }

    public StoryList toStoryList(Slice<Story> slice, Long currentUserId, boolean includeRecentViews) {
        if (slice == null) {
            return new StoryList();
        }

        // Маппим Story в StoryDTO
        List<StoryDTO> storyDTOs = toDTO(slice.getContent(), currentUserId, includeRecentViews);

        // Создаем Slice<StoryDTO> для использования в StoryList.fromSlice()
        Slice<StoryDTO> dtoSlice = new org.springframework.data.domain.SliceImpl<>(
                storyDTOs,
                slice.getPageable(),
                slice.hasNext()
        );

        return StoryList.fromSlice(dtoSlice);
    }

    // ===== Обратный маппинг =====

    public Story toEntity(StoryDTO dto) {
        if (dto == null) {
            return null;
        }

        Story entity = new Story();
        entity.setId(dto.getId());
        entity.setImageId(dto.getImageId());
        entity.setContentType(dto.getContentType());
        entity.setEmoji(dto.getEmoji());
        entity.setDescription(dto.getDescription());
        entity.setExpiresAt(dto.getExpiresAt());
        entity.setViewsCount(dto.getViewsCount());

        // Связанные сущности устанавливаются в сервисе
        // entity.setUser() - в service layer
        // entity.setImageData() - отдельно при загрузке

        return entity;
    }

    // ===== Вспомогательные методы =====

    private String calculateTimeAgo(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Недавно";
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            long minutes = ChronoUnit.MINUTES.between(createdAt, now);
            long hours = ChronoUnit.HOURS.between(createdAt, now);
            long days = ChronoUnit.DAYS.between(createdAt, now);

            if (minutes < 1) {
                return "Только что";
            } else if (minutes < 60) {
                return minutes + " мин. назад";
            } else if (hours < 24) {
                return hours + " ч. назад";
            } else if (days == 1) {
                return "Вчера";
            } else {
                return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            }
        } catch (Exception e) {
            return "Недавно";
        }
    }

    private String calculateExpiresIn(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return "Неизвестно";
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(expiresAt)) {
                return "Истекла";
            }

            long minutes = ChronoUnit.MINUTES.between(now, expiresAt);
            long hours = ChronoUnit.HOURS.between(now, expiresAt);

            if (minutes < 60) {
                return "истекает через " + minutes + " мин.";
            } else if (hours < 24) {
                return "истекает через " + hours + " ч.";
            } else {
                return "истекает " + expiresAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            }
        } catch (Exception e) {
            return "Неизвестно";
        }
    }

    private boolean hasUserViewed(Story story, Long userId) {
        if (story.getViews() == null || userId == null) {
            return false;
        }

        return story.getViews().stream()
                .anyMatch(view -> view.getViewer() != null &&
                        Objects.equals(view.getViewer().getId(), userId));
    }
}