package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.AccessDeniedException;
import com.example.cleopatra.ExistsException.ForumNotFoundException;
import com.example.cleopatra.dto.Forum.ForumCreateDTO;
import com.example.cleopatra.dto.Forum.ForumCreateResponseDTO;
import com.example.cleopatra.dto.Forum.ForumDetailDTO;
import com.example.cleopatra.dto.Forum.ForumPageResponseDTO;
import com.example.cleopatra.enums.ForumType;
import com.example.cleopatra.maper.ForumMapper;
import com.example.cleopatra.model.Forum;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.ForumRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.ForumService;
import com.example.cleopatra.service.UserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {

    private final UserService userService;
    private final ForumRepository forumRepository;
    private final ForumMapper forumMapper;

    @Override
    public ForumCreateResponseDTO createForum(ForumCreateDTO forumCreateDTO, String userEmail) {
        Long userId = userService.getUserIdByEmail(userEmail);
        User user = User.builder().id(userId).build();

        // Используем маппер для создания сущности
        Forum forum = forumMapper.toEntity(forumCreateDTO, user);

        Forum savedForum = forumRepository.save(forum);

        log.info("Создана новая тема форума с ID: {} пользователем: {}", savedForum.getId(), userEmail);

        return forumMapper.toCreateResponseDTO(savedForum, "Тема успешно создана");
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "forums-detailed", key = "#forumId"),
            @CacheEvict(value = "forum-pages", allEntries = true) // Очищаем все страницы
    })
    public void deleteForum(Long forumId, String userEmail, boolean isAdmin) {
        Forum forum = forumRepository.findByIdWithUser(forumId) // ✅ С user
                .orElseThrow(() -> new ForumNotFoundException("Тема с ID " + forumId + " не найдена"));

        if (!isAdmin && !forum.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Нет прав для удаления этой темы");
        }
        forumRepository.delete(forum);
        log.info("Тема '{}' (ID: {}) удалена пользователем: {}",
                forum.getTitle(), forumId, userEmail);
    }


    // Альтернативный вариант с отдельным методом:
    @Cacheable(value = "forums-detailed", key = "#forumId")
    public ForumDetailDTO getForumByIdDetailed(Long forumId) {
        Forum forum = forumRepository.findByIdWithUser(forumId) // ✅ Один запрос с JOIN
                .orElseThrow(() -> new ForumNotFoundException("Тема не найдена"));

        return forumMapper.toDetailDTO(forum);
    }


    @Override
    public ForumDetailDTO viewForum(Long forumId, String userEmail) {
        incrementViewCount(forumId); // Async
        return getForumByIdDetailed(forumId); // Из кеша, но теперь без N+1
    }


    @Async
    public void incrementViewCount(Long forumId) {
        forumRepository.incrementViewCount(forumId);
    }


    @Override
    @Cacheable(value = "forum-pages", key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDirection + '-' + (#forumType != null ? #forumType.name() : 'ALL')")
    public ForumPageResponseDTO getAllForums(int page, int size, String sortBy, String sortDirection, ForumType forumType) {

        // Валидация параметров
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 100); // Максимум 100 элементов
        sortBy = validateSortField(sortBy);
        sortDirection = validateSortDirection(sortDirection);

        // Создание Pageable
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // Получение данных
        Page<Forum> forumPage;
        if (forumType != null) {
            forumPage = forumRepository.findByForumType(forumType, pageable);
        } else {
            forumPage = forumRepository.findAll(pageable);
        }

        log.debug("Загружена страница {} с {} элементами, тип: {}", page, forumPage.getContent().size(), forumType);

        return forumMapper.toPageResponseDTO(forumPage);
    }

    @Override
    @Cacheable(value = "forum-search", key = "#searchQuery + '-' + #page + '-' + #size")
    public ForumPageResponseDTO searchForums(String searchQuery, int page, int size) {

        // Валидация поискового запроса
        if (searchQuery == null || searchQuery.trim().length() < 2) {
            throw new IllegalArgumentException("Поисковый запрос должен содержать минимум 2 символа");
        }

        // Нормализация запроса
        String normalizedQuery = searchQuery.trim().toLowerCase();

        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 50); // Для поиска меньший лимит

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Поиск по заголовку и описанию
        Page<Forum> searchResults = forumRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                normalizedQuery, normalizedQuery, pageable);

        log.info("Поиск '{}' вернул {} результатов", searchQuery, searchResults.getTotalElements());

        return forumMapper.toPageResponseDTO(searchResults);
    }

    private String validateSortField(String sortBy) {
        Set<String> allowedFields = Set.of("createdAt", "title", "viewCount", "commentCount", "forumType");
        return allowedFields.contains(sortBy) ? sortBy : "createdAt";
    }

    private String validateSortDirection(String sortDirection) {
        return "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
    }


}

