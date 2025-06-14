package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.ProjectNews.CreateProjectNewsRequest;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsPageResponse;
import com.example.cleopatra.dto.ProjectNews.ProjectNewsResponse;
import com.example.cleopatra.dto.ProjectNews.UpdateProjectNewsRequest;
import com.example.cleopatra.enums.NewsType;
import com.example.cleopatra.maper.ProjectNewsServiceMapper;
import com.example.cleopatra.maper.WallPostMapper;
import com.example.cleopatra.model.ProjectNews;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.ProjectNewsRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectNewsServiceImpl implements ProjectNewsService {

    private final ProjectNewsRepository projectNewsRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final ImageValidator imageValidator;
    private final ProjectNewsServiceMapper projectNewsServiceMapper;


    /**
     * Создание новости
     */
    @Transactional
    public ProjectNewsResponse createNews(CreateProjectNewsRequest request, Long authorId) {
        log.info("Создание новости: title='{}', authorId={}", request.getTitle(), authorId);

        // 1. Находим автора новости
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> {
                    log.error("Автор новости не найден: authorId={}", authorId);
                    return new IllegalArgumentException("Пользователь не найден с ID: " + authorId);
                });

        // 2. Создаем сущность новости из запроса
        ProjectNews news = projectNewsServiceMapper.toEntity(request, author);

        // 3. Обрабатываем фото, если есть
        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            try {
                handlePhotoUpload(news, request.getPhoto());
            } catch (IOException e) {
                log.error("Ошибка при загрузке фото для новости: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка при загрузке изображения: " + e.getMessage(), e);
            }
        }

        // 4. Устанавливаем время публикации, если новость публикуется сразу
        if (Boolean.TRUE.equals(request.getPublishImmediately())) {
            news.setPublishedAt(LocalDateTime.now());
            log.info("Новость будет опубликована немедленно: title='{}'", request.getTitle());
        } else {
            log.info("Новость сохранена как черновик: title='{}'", request.getTitle());
        }

        // 5. Сохраняем новость в базе данных
        ProjectNews savedNews = projectNewsRepository.save(news);

        log.info("Новость успешно создана: id={}, title='{}', published={}",
                savedNews.getId(), savedNews.getTitle(), savedNews.getIsPublished());

        // 6. Возвращаем DTO ответа
        return projectNewsServiceMapper.toResponse(savedNews);
    }

    /**
     * Обработка загрузки фото для новости
     */
    private void handlePhotoUpload(ProjectNews news, MultipartFile photo) throws IOException {
        log.debug("Начинаем обработку фото для новости: filename='{}'", photo.getOriginalFilename());

        // 1. Валидируем и обрабатываем изображение
        ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(photo);

        // 2. Загружаем обработанное изображение в хранилище
        StorageService.StorageResult storageResult = storageService.uploadProcessedImage(processedImage);

        // 3. Сохраняем информацию о загруженном изображении
        news.setPhotoUrl(storageResult.getUrl());
        news.setPhotoId(storageResult.getImageId());

        log.info("Фото успешно загружено для новости: photoId='{}', url='{}'",
                storageResult.getImageId(), storageResult.getUrl());
    }

    /**
     * Публикация новости (изменение статуса с черновика на опубликованную)
     */
    @Transactional
    public ProjectNewsResponse publishNews(Long newsId) {
        log.info("Публикация новости: newsId={}", newsId);

        ProjectNews news = projectNewsRepository.findById(newsId)
                .orElseThrow(() -> {
                    log.error("Новость не найдена для публикации: newsId={}", newsId);
                    return new IllegalArgumentException("Новость не найдена с ID: " + newsId);
                });

        if (Boolean.TRUE.equals(news.getIsPublished())) {
            log.warn("Попытка опубликовать уже опубликованную новость: newsId={}", newsId);
            throw new IllegalStateException("Новость уже опубликована");
        }

        // Публикуем новость
        news.setIsPublished(true);
        news.setPublishedAt(LocalDateTime.now());

        ProjectNews savedNews = projectNewsRepository.save(news);

        log.info("Новость успешно опубликована: id={}, title='{}'",
                savedNews.getId(), savedNews.getTitle());

        return projectNewsServiceMapper.toResponse(savedNews);
    }

    /**
     * Снятие новости с публикации
     */
    @Transactional
    public ProjectNewsResponse unpublishNews(Long newsId) {
        log.info("Снятие новости с публикации: newsId={}", newsId);

        ProjectNews news = projectNewsRepository.findById(newsId)
                .orElseThrow(() -> {
                    log.error("Новость не найдена для снятия с публикации: newsId={}", newsId);
                    return new IllegalArgumentException("Новость не найдена с ID: " + newsId);
                });

        if (!Boolean.TRUE.equals(news.getIsPublished())) {
            log.warn("Попытка снять с публикации неопубликованную новость: newsId={}", newsId);
            throw new IllegalStateException("Новость не опубликована");
        }

        // Снимаем с публикации
        news.setIsPublished(false);
        // publishedAt оставляем как есть для истории

        ProjectNews savedNews = projectNewsRepository.save(news);

        log.info("Новость успешно снята с публикации: id={}, title='{}'",
                savedNews.getId(), savedNews.getTitle());

        return projectNewsServiceMapper.toResponse(savedNews);
    }

    /**
     * Увеличение счетчика просмотров
     */
    @Transactional
    public void incrementViewCount(Long newsId) {
        log.debug("Увеличение счетчика просмотров: newsId={}", newsId);

        // Используем модифицирующий запрос для оптимизации
        projectNewsRepository.incrementViewCount(newsId);

        log.debug("Счетчик просмотров увеличен для новости: newsId={}", newsId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectNewsResponse getPublishedNewsById(Long id) {
        log.info("Получение опубликованной новости по ID: {}", id);

        ProjectNews news = projectNewsRepository.findByIdAndIsPublishedTrue(id)
                .orElseThrow(() -> {
                    log.error("Опубликованная новость не найдена: id={}", id);
                    return new IllegalArgumentException("Опубликованная новость не найдена с ID: " + id);
                });

        log.debug("Опубликованная новость найдена: id={}, title='{}', views={}",
                news.getId(), news.getTitle(), news.getViewsCount());

        return projectNewsServiceMapper.toResponse(news);
    }

    @Override
    public ProjectNewsPageResponse getAllNewsForAdmin(int page, int size, NewsType newsType, Boolean isPublished) {
        log.info("Получение новостей для админки: page={}, size={}, newsType={}, isPublished={}",
                page, size, newsType, isPublished);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Slice<ProjectNews> newsSlice;

        if (newsType != null && isPublished != null) {
            // Фильтр по типу и статусу публикации
            if (isPublished) {
                newsSlice = projectNewsRepository.findByIsPublishedTrueAndNewsTypeOrderByPublishedAtDesc(newsType, pageable);
            } else {
                newsSlice = projectNewsRepository.findByIsPublishedFalseAndNewsTypeOrderByCreatedAtDesc(newsType, pageable);
            }
        } else if (newsType != null) {
            // Только по типу
            newsSlice = projectNewsRepository.findByNewsTypeOrderByCreatedAtDesc(newsType, pageable);
        } else if (isPublished != null) {
            // Только по статусу публикации
            newsSlice = projectNewsRepository.findByIsPublishedOrderByCreatedAtDesc(isPublished, pageable);
        } else {
            // Все новости
            newsSlice = projectNewsRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        log.debug("Найдено новостей для админки: {}", newsSlice.getNumberOfElements());

        return projectNewsServiceMapper.toPageResponse(newsSlice);
    }

    @Override
    public ProjectNewsResponse getNewsById(Long id) {
        log.info("Получение новости по ID: {}", id);

        ProjectNews news = projectNewsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Новость не найдена: id={}", id);
                    return new IllegalArgumentException("Новость не найдена с ID: " + id);
                });

        log.debug("Новость найдена: id={}, title='{}', published={}",
                news.getId(), news.getTitle(), news.getIsPublished());

        return projectNewsServiceMapper.toResponse(news);
    }

    @Override
    @Transactional
    public ProjectNewsResponse updateNews(Long id, UpdateProjectNewsRequest request) {
        log.info("Обновление новости: id={}, title='{}'", id, request.getTitle());

        ProjectNews news = projectNewsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Новость не найдена для обновления: id={}", id);
                    return new IllegalArgumentException("Новость не найдена с ID: " + id);
                });

        // Сохраняем старые значения для логирования
        String oldTitle = news.getTitle();
        String oldPhotoId = news.getPhotoId();

        // Обновляем поля из запроса
        projectNewsServiceMapper.updateEntityFromRequest(news, request);

        // Обрабатываем фото
        try {
            if (Boolean.TRUE.equals(request.getRemovePhoto())) {
                // Удаляем старое фото из хранилища, если есть
                if (oldPhotoId != null) {
                    storageService.deleteImage(oldPhotoId);
                    log.info("Удалено старое фото: photoId='{}'", oldPhotoId);
                }
                news.setPhotoUrl(null);
                news.setPhotoId(null);
                log.info("Фото удалено из новости: id={}", id);

            } else if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
                // Удаляем старое фото, если есть
                if (oldPhotoId != null) {
                    storageService.deleteImage(oldPhotoId);
                    log.info("Удалено старое фото при замене: photoId='{}'", oldPhotoId);
                }
                // Загружаем новое
                handlePhotoUpload(news, request.getPhoto());
                log.info("Загружено новое фото для новости: id={}", id);
            }
        } catch (IOException e) {
            log.error("Ошибка при обработке фото новости id={}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Ошибка при обработке изображения: " + e.getMessage(), e);
        }

        // Сохраняем обновленную новость
        ProjectNews savedNews = projectNewsRepository.save(news);

        log.info("Новость успешно обновлена: id={}, oldTitle='{}', newTitle='{}'",
                id, oldTitle, savedNews.getTitle());

        return projectNewsServiceMapper.toResponse(savedNews);
    }

    @Override
    @Transactional
    public void deleteNews(Long id) {
        log.info("Удаление новости: id={}", id);

        ProjectNews news = projectNewsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Новость не найдена для удаления: id={}", id);
                    return new IllegalArgumentException("Новость не найдена с ID: " + id);
                });

        String title = news.getTitle();
        String photoId = news.getPhotoId();

        // Удаляем фото из хранилища, если есть
        if (photoId != null) {
            try {
                boolean deleted = storageService.deleteImage(photoId);
                if (deleted) {
                    log.info("Фото успешно удалено из хранилища: photoId='{}'", photoId);
                } else {
                    log.warn("Не удалось удалить фото из хранилища: photoId='{}'", photoId);
                }
            } catch (Exception e) {
                log.warn("Ошибка при удалении фото из хранилища: photoId='{}', error={}",
                        photoId, e.getMessage());
                // Продолжаем удаление новости, даже если не удалось удалить фото
            }
        }

        // Удаляем новость из базы данных
        projectNewsRepository.delete(news);

        log.info("Новость успешно удалена: id={}, title='{}'", id, title);
    }

    @Override
    public ProjectNewsPageResponse getPublishedNews(int page, int size, NewsType newsType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));

        Slice<ProjectNews> newsSlice;
        if (newsType != null) {
            // Фильтрация по типу
            newsSlice = projectNewsRepository.findByIsPublishedTrueAndNewsTypeOrderByPublishedAtDesc(newsType, pageable);
        } else {
            // Все опубликованные новости
            newsSlice = projectNewsRepository.findByIsPublishedTrueOrderByPublishedAtDesc(pageable);
        }

        return projectNewsServiceMapper.toPageResponse(newsSlice);
    }
    }
