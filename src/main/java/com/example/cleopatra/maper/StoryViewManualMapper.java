package com.example.cleopatra.maper;

import com.example.cleopatra.dto.StoryView.StoryViewDTO;
import com.example.cleopatra.model.StoryView;
import com.example.cleopatra.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StoryViewManualMapper {

    public StoryViewDTO toDTO(StoryView storyView) {
        if (storyView == null) {
            return null;
        }

        User viewer = storyView.getViewer();

        return StoryViewDTO.builder()
                .id(storyView.getId())
                .storyId(storyView.getStory().getId())
                .viewerId(viewer.getId())
                .viewerFirstName(viewer.getFirstName())
                .viewerLastName(viewer.getLastName())
                .viewerImageUrl(viewer.getImageUrl())
                .viewerImgId(viewer.getImgId())
                .viewedAt(storyView.getViewedAt())
                .createdAt(storyView.getCreatedAt())
                .build();
    }

    public List<StoryViewDTO> toDTO(List<StoryView> storyViews) {
        if (storyViews == null) {
            return null;
        }

        return storyViews.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Дополнительный метод для создания DTO с минимальной информацией (для оптимизации)
    public StoryViewDTO toDTOMinimal(StoryView storyView) {
        if (storyView == null) {
            return null;
        }

        User viewer = storyView.getViewer();

        return StoryViewDTO.builder()
                .id(storyView.getId())
                .viewerId(viewer.getId())
                .viewerFirstName(viewer.getFirstName())
                .viewerLastName(viewer.getLastName())
                .viewerImageUrl(viewer.getImageUrl())
                .viewedAt(storyView.getViewedAt())
                .build();
    }
}