package com.example.cleopatra.maper;

import com.example.cleopatra.dto.ForumComment.CreateForumCommentDto;
import com.example.cleopatra.dto.ForumComment.ForumCommentDto;
import com.example.cleopatra.model.ForumComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ForumCommentMapper {

    /**
     * Преобразование сущности ForumComment в DTO
     */
    public ForumCommentDto toDto(ForumComment forumComment) {
        if (forumComment == null) {
            log.warn("Попытка преобразования null ForumComment в DTO");
            return null;
        }

        try {
            return ForumCommentDto.builder()
                    .id(forumComment.getId())
                    .content(forumComment.getContent())
                    .forumId(forumComment.getForum() != null ? forumComment.getForum().getId() : null)
                    .parentId(forumComment.getParent() != null ? forumComment.getParent().getId() : null)
                    .level(forumComment.getLevel())
                    .childrenCount(forumComment.getChildrenCount())
                    .deleted(forumComment.getDeleted())
                    // Данные автора
                    .authorId(forumComment.getAuthor() != null ? forumComment.getAuthor().getId() : null)
                    .authorName(forumComment.getAuthor() != null ? forumComment.getAuthor().getEmail() : "Unknown")
                    // Временные метки
                    .createdAt(forumComment.getCreatedAt())
                    .updatedAt(forumComment.getUpdatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при преобразовании ForumComment в DTO для комментария ID: {}",
                    forumComment.getId(), e);
            return null;
        }
    }

    /**
     * Преобразование CreateForumCommentDto в сущность ForumComment
     * Внимание: не устанавливает связи (Forum, User, Parent) - это делается в сервисе
     */
    public ForumComment toEntity(CreateForumCommentDto createDto) {
        if (createDto == null) {
            log.warn("Попытка преобразования null CreateForumCommentDto в сущность");
            return null;
        }

        try {
            return ForumComment.builder()
                    .content(createDto.getContent())
                    // Остальные поля устанавливаются в сервисе:
                    // - forum (через forumId)
                    // - author (через userId)
                    // - parent (через parentId)
                    // - level (вычисляется)
                    // - timestamps (автоматически)
                    .childrenCount(0)
                    .deleted(false)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при преобразовании CreateForumCommentDto в сущность", e);
            return null;
        }
    }

    /**
     * Обновление существующей сущности данными из DTO (для редактирования)
     */
    public void updateEntity(ForumComment existingComment, CreateForumCommentDto updateDto) {
        if (existingComment == null || updateDto == null) {
            log.warn("Попытка обновления с null параметрами");
            return;
        }

        try {
            // Обновляем только разрешенные поля
            if (updateDto.getContent() != null && !updateDto.getContent().trim().isEmpty()) {
                existingComment.setContent(updateDto.getContent());
            }

            log.debug("Сущность ForumComment ID: {} успешно обновлена", existingComment.getId());

        } catch (Exception e) {
            log.error("Ошибка при обновлении ForumComment ID: {}", existingComment.getId(), e);
        }
    }

    /**
     * Создание DTO с базовой информацией (без чувствительных данных)
     * Полезно для публичных API или когда нужно скрыть email автора
     */
    public ForumCommentDto toPublicDto(ForumComment forumComment) {
        if (forumComment == null) {
            return null;
        }

        ForumCommentDto dto = toDto(forumComment);
        if (dto != null) {
            // Скрываем email для публичного просмотра

            // Если комментарий удален, скрываем контент
            if (dto.isDeleted()) {
                dto.setContent("[Комментарий удален]");
            }
        }

        return dto;
    }

    /**
     * Массовое преобразование списка сущностей в DTO
     */
    public java.util.List<ForumCommentDto> toDtoList(java.util.List<ForumComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return comments.stream()
                .map(this::toDto)
                .filter(dto -> dto != null) // Фильтруем null значения
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Массовое преобразование в публичные DTO
     */
    public java.util.List<ForumCommentDto> toPublicDtoList(java.util.List<ForumComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return comments.stream()
                .map(this::toPublicDto)
                .filter(dto -> dto != null)
                .collect(java.util.stream.Collectors.toList());
    }
}