package com.example.cleopatra.maper;

import com.example.cleopatra.dto.ForumComment.CreateForumCommentDto;
import com.example.cleopatra.dto.ForumComment.ForumCommentDto;
import com.example.cleopatra.model.ForumComment;
import com.example.cleopatra.model.User;
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
            // ✅ Безопасное получение данных автора
            Long authorId = null;
            String authorName = "Удаленный пользователь";
            String authorAvatar = null;

            if (forumComment.getAuthor() != null) {
                User author = forumComment.getAuthor();
                authorId = author.getId();

                // ✅ ИСПРАВЛЕНО: используем имя, а не email
                if (author.getFirstName() != null && !author.getFirstName().trim().isEmpty()) {
                    authorName = author.getFirstName();
                    if (author.getLastName() != null && !author.getLastName().trim().isEmpty()) {
                        authorName += " " + author.getLastName();
                    }
                } else {
                    authorName = author.getEmail(); // fallback на email
                }

                // ✅ ИСПРАВЛЕНО: безопасное получение аватара
                authorAvatar = author.getImageUrl(); // или getProfileImageUrl() - как у вас называется
            }

            return ForumCommentDto.builder()
                    .id(forumComment.getId())
                    .content(forumComment.getContent())
                    .forumId(forumComment.getForum() != null ? forumComment.getForum().getId() : null)
                    .parentId(forumComment.getParent() != null ? forumComment.getParent().getId() : null)
                    .level(forumComment.getLevel())
                    .childrenCount(forumComment.getChildrenCount())
                    .deleted(forumComment.getDeleted())

                    // ✅ Данные автора
                    .authorId(authorId)           // ← ID уже есть!
                    .authorName(authorName)       // ← улучшенное имя
                    .authorAvatar(authorAvatar)   // ← фото уже есть!

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



}