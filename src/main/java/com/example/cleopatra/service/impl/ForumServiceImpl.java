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
import com.example.cleopatra.service.ForumReadService;
import com.example.cleopatra.service.ForumService;
import com.example.cleopatra.service.UserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

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
    private final ForumReadService forumReadService;

    @Override
    @CacheEvict(value = {"forum-pages", "forum-count"}, allEntries = true)
    public ForumCreateResponseDTO createForum(ForumCreateDTO forumCreateDTO, String userEmail) {
        Long userId = userService.getUserIdByEmail(userEmail);
        User user = User.builder().id(userId).build();

        Forum forum = forumMapper.toEntity(forumCreateDTO, user);
        Forum savedForum = forumRepository.save(forum);

        log.info("Создана новая тема форума с ID: {} пользователем: {}", savedForum.getId(), userEmail);
        return forumMapper.toCreateResponseDTO(savedForum, "Тема успешно создана");
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "forums-detailed", key = "#forumId"),
            @CacheEvict(value = "forum-pages", allEntries = true),
            @CacheEvict(value = "forum-search", allEntries = true)
    })
    public void deleteForum(Long forumId, String userEmail, boolean isAdmin) {
        Forum forum = forumRepository.findByIdWithUser(forumId)
                .orElseThrow(() -> new ForumNotFoundException("Тема с ID " + forumId + " не найдена"));

        if (!isAdmin && !forum.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Нет прав для удаления этой темы");
        }

        forumRepository.delete(forum);
        log.info("Тема '{}' (ID: {}) удалена пользователем: {}", forum.getTitle(), forumId, userEmail);
    }

    @Override
    public ForumDetailDTO viewForum(Long forumId, String userEmail) {
        incrementViewCount(forumId);
        return forumReadService.getForumById(forumId);
    }

    @Async
    public void incrementViewCount(Long forumId) {
        forumRepository.incrementViewCount(forumId);
    }
}