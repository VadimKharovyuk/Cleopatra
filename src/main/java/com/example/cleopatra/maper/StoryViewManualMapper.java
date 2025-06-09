package com.example.cleopatra.maper;

import com.example.cleopatra.dto.StoryView.StoryViewDTO;
import com.example.cleopatra.model.StoryView;
import com.example.cleopatra.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StoryViewManualMapper {

    public StoryViewDTO toDTO(StoryView storyView) {
        if (storyView == null) {
            return null;
        }

        StoryViewDTO dto = new StoryViewDTO();
        dto.setId(storyView.getId());

        // Маппинг storyId с проверкой на null
        if (storyView.getStory() != null) {
            dto.setStoryId(storyView.getStory().getId());
        }

        // Маппинг данных viewer с проверками на null
        User viewer = storyView.getViewer();
        if (viewer != null) {
            dto.setViewerId(viewer.getId());
            dto.setViewerFirstName(viewer.getFirstName());
            dto.setViewerLastName(viewer.getLastName());
            dto.setViewerImageUrl(viewer.getImageUrl());
        }

        // Маппинг времени просмотра
        dto.setViewedAt(storyView.getViewedAt());
        dto.setTimeAgo(calculateTimeAgo(storyView.getViewedAt()));

        return dto;
    }

    public List<StoryViewDTO> toDTO(List<StoryView> storyViews) {
        if (storyViews == null || storyViews.isEmpty()) {
            return Collections.emptyList();
        }

        return storyViews.stream()
                .filter(Objects::nonNull) // Дополнительная защита от null
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public StoryView toEntity(StoryViewDTO dto) {
        if (dto == null) {
            return null;
        }

        StoryView entity = new StoryView();
        entity.setId(dto.getId());
        entity.setViewedAt(dto.getViewedAt());

        // Связанные сущности устанавливаются в сервисе
        // entity.setStory() и entity.setViewer() - в service layer

        return entity;
    }

    private String calculateTimeAgo(LocalDateTime viewedAt) {
        if (viewedAt == null) {
            return "Недавно";
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            long minutes = ChronoUnit.MINUTES.between(viewedAt, now);
            long hours = ChronoUnit.HOURS.between(viewedAt, now);
            long days = ChronoUnit.DAYS.between(viewedAt, now);

            if (minutes < 1) {
                return "Только что";
            } else if (minutes < 60) {
                return minutes + " мин. назад";
            } else if (hours < 24) {
                return hours + " ч. назад";
            } else if (days == 1) {
                return "Вчера";
            } else {
                // Более красивый формат даты
                return viewedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            }
        } catch (Exception e) {
            return "Недавно"; // Fallback при любых ошибках
        }
    }
}