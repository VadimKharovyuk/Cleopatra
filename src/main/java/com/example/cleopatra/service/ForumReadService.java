package com.example.cleopatra.service;

import com.example.cleopatra.dto.Forum.ForumDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.cleopatra.ExistsException.ForumNotFoundException;
import com.example.cleopatra.dto.Forum.ForumDetailDTO;
import com.example.cleopatra.dto.Forum.ForumPageCardDTO;
import com.example.cleopatra.dto.Forum.ForumPageResponseDTO;
import com.example.cleopatra.enums.ForumType;
import com.example.cleopatra.maper.ForumMapper;
import com.example.cleopatra.model.Forum;
import com.example.cleopatra.repository.ForumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;
@Slf4j
@Service
@RequiredArgsConstructor
public class ForumReadService {

    private final ForumRepository forumRepository;
    private final ForumMapper forumMapper;

    @Cacheable(value = "forums-detailed", key = "#forumId")
    public ForumDetailDTO getForumById(Long forumId) {
        log.info("🔍 ForumReadService.getForumById: {}", forumId);

        Forum forum = forumRepository.findByIdWithUser(forumId)
                .orElseThrow(() -> {
                    log.warn("⚠️ Тема с ID {} не найдена в БД", forumId);
                    return new ForumNotFoundException("Тема не найдена");
                });

        log.info("📋 Найдена тема в БД: {}", forum.getTitle());

        ForumDetailDTO result = forumMapper.toDetailDTO(forum);
        log.info("📋 Результат маппинга: {}", result != null ? result.getTitle() : "NULL");

        return result;
    }

    @Cacheable(value = "forum-pages", key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDirection + '-' + (#forumType != null ? #forumType.name() : 'ALL')")
    public ForumPageResponseDTO getAllForums(int page, int size, String sortBy, String sortDirection, ForumType forumType) {

        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 100);
        sortBy = validateSortField(sortBy);
        sortDirection = validateSortDirection(sortDirection);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Forum> forumPage;
        if (forumType != null) {
            // ✅ ИСПРАВЛЕНО: убрали WithUser из названия
            forumPage = forumRepository.findByForumType(forumType, pageable);
        } else {
            // ✅ ИСПРАВЛЕНО: используем стандартный findAll
            forumPage = forumRepository.findAll(pageable);
        }

        log.debug("Загружена страница {} с {} элементами, тип: {}", page, forumPage.getContent().size(), forumType);
        return forumMapper.toPageResponseDTO(forumPage);
    }

    @Cacheable(value = "forum-search", key = "#searchQuery + '-' + #page + '-' + #size")
    public ForumPageResponseDTO searchForums(String searchQuery, int page, int size) {

        if (searchQuery == null || searchQuery.trim().length() < 2) {
            throw new IllegalArgumentException("Поисковый запрос должен содержать минимум 2 символа");
        }

        String normalizedQuery = searchQuery.trim().toLowerCase();
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 50);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // ✅ ИСПРАВЛЕНО: убрали WithUser из названия
        Page<Forum> searchResults = forumRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                normalizedQuery, normalizedQuery, pageable);

        log.info("Поиск '{}' вернул {} результатов", searchQuery, searchResults.getTotalElements());
        return forumMapper.toPageResponseDTO(searchResults);
    }


    @Cacheable(value = "forum-exists", key = "#forumId")
    public boolean existsById(Long forumId) {
        return forumRepository.existsById(forumId);
    }

    @Cacheable(value = "forum-count", key = "#forumType != null ? #forumType.name() : 'ALL'")
    public long getForumCountByType(ForumType forumType) {
        if (forumType != null) {
            return forumRepository.countByForumType(forumType);
        }
        return forumRepository.count();
    }

    private String validateSortField(String sortBy) {
        Set<String> allowedFields = Set.of("createdAt", "title", "viewCount", "commentCount", "forumType");
        return allowedFields.contains(sortBy) ? sortBy : "createdAt";
    }

    private String validateSortDirection(String sortDirection) {
        return "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
    }
}