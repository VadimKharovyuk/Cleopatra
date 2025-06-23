package com.example.cleopatra.service.impl;
import com.example.cleopatra.dto.ForumComment.CreateForumCommentDto;
import com.example.cleopatra.dto.ForumComment.ForumCommentDto;
import com.example.cleopatra.dto.ForumComment.ForumCommentPageDto;
import com.example.cleopatra.maper.ForumCommentMapper;
import com.example.cleopatra.model.Forum;
import com.example.cleopatra.model.ForumComment;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.ForumCommentRepository;
import com.example.cleopatra.repository.ForumRepository;
import com.example.cleopatra.service.ForumCommentService;
import com.example.cleopatra.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class ForumCommentServiceImpl implements ForumCommentService {

    private final ForumCommentRepository forumCommentRepository;
    private final UserService userService;
    private final ForumRepository forumRepository;
    private final ForumCommentMapper forumCommentMapper;

    @Override
    @Transactional
    public ForumCommentDto createForumComment(CreateForumCommentDto forumCommentDto, Long userId) {
        log.info("Создание комментария для форума ID: {} пользователем ID: {}",
                forumCommentDto.getForumId(), userId);

        // Валидация и получение форума
        Forum forum = forumRepository.findById(forumCommentDto.getForumId())
                .orElseThrow(() -> {
                    log.error("Форум с ID {} не найден", forumCommentDto.getForumId());
                    return new EntityNotFoundException("Форум не найден");
                });

        // Получение пользователя
        User currentUser = userService.findById(userId);
        if (currentUser == null) {
            log.error("Пользователь с ID {} не найден", userId);
            throw new EntityNotFoundException("Пользователь не найден");
        }

        // Валидация родительского комментария (если это ответ)
        ForumComment parentComment = null;
        if (forumCommentDto.getParentId() != null) {
            parentComment = forumCommentRepository.findById(forumCommentDto.getParentId())
                    .orElseThrow(() -> {
                        log.error("Родительский комментарий с ID {} не найден", forumCommentDto.getParentId());
                        return new EntityNotFoundException("Родительский комментарий не найден");
                    });

            // Проверяем что родительский комментарий относится к тому же форуму
            if (!parentComment.getForum().getId().equals(forum.getId())) {
                log.error("Родительский комментарий {} не относится к форуму {}",
                        forumCommentDto.getParentId(), forum.getId());
                throw new IllegalArgumentException("Родительский комментарий не относится к данному форуму");
            }

            // Проверяем что родительский комментарий не удален
            if (parentComment.getDeleted()) {
                log.error("Попытка ответить на удаленный комментарий ID: {}", forumCommentDto.getParentId());
                throw new IllegalArgumentException("Нельзя отвечать на удаленный комментарий");
            }
        }

        // Создание сущности комментария
        ForumComment comment = ForumComment.builder()
                .content(forumCommentDto.getContent())
                .forum(forum)
                .author(currentUser)
                .parent(parentComment)
                .level(parentComment != null ? parentComment.getLevel() + 1 : 0)
                .childrenCount(0)
                .deleted(false)
                .build();

        // Сохранение комментария
        ForumComment savedComment = forumCommentRepository.save(comment);
        log.info("Комментарий создан с ID: {}", savedComment.getId());

        // Обновление счетчиков
        updateCounters(forum, parentComment);

        // Преобразование в DTO
        return forumCommentMapper.toDto(savedComment);
    }
    @Override
    @Transactional
    public boolean deleteForumComment(Long forumCommentId, Long userId) {
        log.info("Попытка удаления комментария ID: {} пользователем ID: {}", forumCommentId, userId);

        try {
            // ✅ ОПТИМИЗАЦИЯ: Используем JOIN FETCH для загрузки автора
            Optional<ForumComment> commentOpt = forumCommentRepository.findByIdWithAuthor(forumCommentId);

            if (commentOpt.isEmpty()) {
                log.warn("Комментарий с ID {} не найден", forumCommentId);
                return false;
            }

            ForumComment comment = commentOpt.get();
            // Автор уже загружен благодаря JOIN FETCH - никаких дополнительных запросов!

            // Проверка прав (автор или админ)
            if (!comment.getAuthor().getId().equals(userId)) {
                User currentUser = userService.findById(userId);
                if (currentUser == null || !currentUser.getRole().name().equals("ADMIN")) {
                    log.warn("Пользователь ID: {} не имеет прав на удаление комментария ID: {}", userId, forumCommentId);
                    return false;
                }
            }

            // Проверка что комментарий уже не удален
            if (comment.getDeleted()) {
                log.warn("Комментарий ID: {} уже удален", forumCommentId);
                return false;
            }

            // Мягкое удаление
            comment.setDeleted(true);
            forumCommentRepository.save(comment);

            // ✅ ОПТИМИЗАЦИЯ: Используем batch операции для счетчиков
            forumRepository.decrementCommentCount(comment.getForum().getId());

            if (comment.getParent() != null) {
                forumCommentRepository.decrementChildrenCount(comment.getParent().getId());
            }


            return true;

        } catch (Exception e) {
            log.error("Ошибка при удалении комментария ID: {}", forumCommentId, e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true) // ✅ ОПТИМИЗАЦИЯ: read-only транзакция
    public ForumCommentPageDto getForumComments(Long forumId, int page, int size) {

        // Проверяем существование форума
        if (!forumRepository.existsById(forumId)) {
            log.error("Форум с ID {} не найден", forumId);
            throw new EntityNotFoundException("Форум не найден");
        }

        // Создаем Pageable внутри сервиса
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        // ✅ ОПТИМИЗАЦИЯ: Используем JOIN FETCH для авторов
        Slice<ForumComment> commentsSlice = forumCommentRepository
                .findByForumIdWithAuthors(forumId, pageable);

        // Преобразуем в DTO
        List<ForumCommentDto> commentDtos = commentsSlice.getContent()
                .stream()
                .map(forumCommentMapper::toDto)
                .collect(Collectors.toList());

        // Возвращаем наш собственный DTO
        return ForumCommentPageDto.builder()
                .comments(commentDtos)
                .hasNext(commentsSlice.hasNext())
                .hasPrevious(commentsSlice.hasPrevious())
                .numberOfElements(commentsSlice.getNumberOfElements())
                .size(commentsSlice.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true) // ✅ ОПТИМИЗАЦИЯ: read-only транзакция
    public List<ForumCommentDto> getCommentReplies(Long parentCommentId) {

        // ✅ ОПТИМИЗАЦИЯ: Используем JOIN FETCH для авторов
        List<ForumComment> replies = forumCommentRepository
                .findRepliesWithAuthors(parentCommentId);

        if (replies.isEmpty()) {
            // Проверяем существование родительского комментария только если нет ответов
            if (!forumCommentRepository.existsById(parentCommentId)) {
                log.error("Родительский комментарий с ID {} не найден", parentCommentId);
                throw new EntityNotFoundException("Родительский комментарий не найден");
            }
        }

        return replies.stream()
                .map(forumCommentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Обновление счетчиков после создания комментария
     */
    private void updateCounters(Forum forum, ForumComment parentComment) {
        // Обновляем счетчик комментариев в форуме
        forum.setCommentCount(forum.getCommentCount() + 1);
        forumRepository.save(forum);

        // Обновляем счетчик детей у родительского комментария
        if (parentComment != null) {
            parentComment.setChildrenCount(parentComment.getChildrenCount() + 1);
            forumCommentRepository.save(parentComment);
        }

        log.info("Счетчики обновлены для форума ID: {}", forum.getId());
    }
}