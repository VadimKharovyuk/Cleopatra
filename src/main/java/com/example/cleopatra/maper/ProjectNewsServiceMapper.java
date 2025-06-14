package com.example.cleopatra.maper;

import com.example.cleopatra.dto.ProjectNews.CreateProjectNewsRequest;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsPageResponse;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsResponse;
import com.example.cleopatra.model.ProjectNews;
import com.example.cleopatra.model.User;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProjectNewsServiceMapper {

    /**
     * Маппинг User в AuthorInfo для карточки новости
     */
    public ProjectNewsResponse.AuthorInfo toAuthorInfo(User user) {
        if (user == null) {
            return null;
        }

        return ProjectNewsResponse.AuthorInfo.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }

    /**
     * Маппинг User в AuthorInfo для полной новости
     */
    public ProjectNewsResponse.AuthorInfo toFullAuthorInfo(User user) {
        if (user == null) {
            return null;
        }

        return ProjectNewsResponse.AuthorInfo.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }

    /**
     * Маппинг ProjectNews в ProjectNewsCardResponse
     */
    public ProjectNewsResponse toCardResponse(ProjectNews news) {
        if (news == null) {
            return null;
        }

        return ProjectNewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .shortDescription(news.getShortDescription())
                .photoUrl(news.getPhotoUrl())
                .newsType(news.getNewsType())
                .newsTypeDisplayName(news.getNewsType() != null ? news.getNewsType().getDisplayName() : null)
                .viewsCount(news.getViewsCount())
                .isPublished(news.getIsPublished())
                .author(toAuthorInfo(news.getAuthor()))
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt())
                .build();
    }

    /**
     * Маппинг ProjectNews в ProjectNewsResponse (полная информация)
     */
    public ProjectNewsResponse toResponse(ProjectNews news) {
        if (news == null) {
            return null;
        }

        return ProjectNewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .description(news.getDescription())
                .shortDescription(news.getShortDescription())
                .photoUrl(news.getPhotoUrl())
                .photoId(news.getPhotoId())
                .newsType(news.getNewsType())
                .newsTypeDisplayName(news.getNewsType() != null ? news.getNewsType().getDisplayName() : null)
                .viewsCount(news.getViewsCount())
                .isPublished(news.getIsPublished())
                .author(toFullAuthorInfo(news.getAuthor()))
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .build();
    }

    /**
     * Маппинг списка ProjectNews в список ProjectNewsCardResponse
     */
    public List<ProjectNewsResponse> toCardResponseList(List<ProjectNews> newsList) {
        if (newsList == null) {
            return List.of();
        }

        return newsList.stream()
                .map(this::toCardResponse)
                .collect(Collectors.toList());
    }

    /**
     * Маппинг Slice<ProjectNews> в ProjectNewsPageResponse
     */
    public ProjectNewsPageResponse toPageResponse(Slice<ProjectNews> newsSlice) {
        if (newsSlice == null) {
            return ProjectNewsPageResponse.builder()
                    .content(List.of())
                    .hasNext(false)
                    .currentPage(0)
                    .size(0)
                    .isEmpty(true)
                    .numberOfElements(0)
                    .totalElements(0L)
                    .totalPages(0)
                    .build();
        }

        List<ProjectNewsResponse> content = toCardResponseList(newsSlice.getContent());

        return ProjectNewsPageResponse.builder()
                .content(content)
                .hasNext(newsSlice.hasNext())
                .currentPage(newsSlice.getNumber())
                .size(newsSlice.getSize())
                .isEmpty(newsSlice.isEmpty())
                .numberOfElements(newsSlice.getNumberOfElements())
                // Для Slice totalElements и totalPages недоступны, поэтому ставим null или считаем приблизительно
                .totalElements(null) // Или можно убрать из DTO для Slice
                .totalPages(null)    // Или можно убрать из DTO для Slice
                .build();
    }

    /**
     * Маппинг CreateProjectNewsRequest в ProjectNews
     */
    public ProjectNews toEntity(CreateProjectNewsRequest request, User author) {
        if (request == null) {
            return null;
        }

        return ProjectNews.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .newsType(request.getNewsType())
                .author(author)
                .isPublished(Boolean.TRUE.equals(request.getPublishImmediately()))
                .viewsCount(0L)
                .publishedAt(Boolean.TRUE.equals(request.getPublishImmediately()) ? LocalDateTime.now() : null)
                .build();
    }

    /**
     * Обновление ProjectNews из UpdateProjectNewsRequest
     */
    public void updateEntityFromRequest(ProjectNews news, com.example.cleopatra.dto.ProjectNews.UpdateProjectNewsRequest request) {
        if (news == null || request == null) {
            return;
        }

        news.setTitle(request.getTitle());
        news.setDescription(request.getDescription());
        news.setShortDescription(request.getShortDescription());
        news.setNewsType(request.getNewsType());

        // Если нужно удалить фото
        if (Boolean.TRUE.equals(request.getRemovePhoto())) {
            news.setPhotoUrl(null);
            news.setPhotoId(null);
        }
    }
}