package com.example.cleopatra.service;

import com.example.cleopatra.dto.ProjectNews.CreateProjectNewsRequest;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsPageResponse;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsResponse;
import com.example.cleopatra.dto.ProjectNews.UpdateProjectNewsRequest;
import com.example.cleopatra.enums.NewsType;
import jakarta.validation.Valid;

public interface ProjectNewsService {

    ProjectNewsResponse createNews(CreateProjectNewsRequest request, Long authorId);
    ProjectNewsPageResponse getPublishedNews(int page, int size, NewsType newsType);

    ProjectNewsResponse publishNews(Long newsId);
    ProjectNewsResponse unpublishNews(Long newsId);
    void incrementViewCount(Long newsId);

    ProjectNewsResponse getPublishedNewsById(Long id);


    ProjectNewsPageResponse getAllNewsForAdmin(int page, int size, NewsType newsType, Boolean isPublished);


    ProjectNewsResponse getNewsById(Long id);


    ProjectNewsResponse updateNews(Long id, @Valid UpdateProjectNewsRequest request);


    void deleteNews(Long id);
}
