package com.example.cleopatra.maper;
import com.example.cleopatra.dto.Forum.*;
import com.example.cleopatra.model.Forum;
import com.example.cleopatra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ForumMapper {


    public Forum toEntity(ForumCreateDTO dto, User user) {
        if (dto == null || user == null) {
            return null;
        }
        return Forum.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .forumType(dto.getForumType())
                .user(user)
                .build();
    }

    public ForumCreateResponseDTO toCreateResponseDTO(Forum savedForum, String message) {
        if (savedForum == null) {
            return null;
        }

        return ForumCreateResponseDTO.builder()
                .id(savedForum.getId())
                .title(savedForum.getTitle())
                .forumType(savedForum.getForumType())
                .createdAt(savedForum.getCreatedAt())
                .message(message != null ? message : "Операция выполнена")
                .build();
    }
    public ForumDetailDTO toDetailDTO(Forum forum) {
        if (forum == null) {
            return null;
        }

        // Значения по умолчанию
        String authorName = "Неизвестный автор";
        String authorEmail = null;
        String autorImageUrl = null;  // ✅ ДОБАВЛЕНО
        Long authorId = null;         // ✅ ДОБАВЛЕНО

        if (forum.getUser() != null) {
            User user = forum.getUser();

            // Имя автора
            if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
                authorName = user.getFirstName();
                if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
                    authorName += " " + user.getLastName();
                }
            } else {
                authorName = user.getEmail();
            }

            authorEmail = user.getEmail();
            authorId = user.getId();                    // ✅ ДОБАВЛЕНО
            autorImageUrl = user.getImageUrl();  // ✅ ДОБАВЛЕНО (или как называется поле)
        }


        return ForumDetailDTO.builder()
                .id(forum.getId())
                .title(forum.getTitle())
                .description(forum.getDescription())
                .forumType(forum.getForumType())
                .viewCount(forum.getViewCount())
                .commentCount(forum.getCommentCount())
                .createdAt(forum.getCreatedAt())
                .updatedAt(forum.getUpdatedAt())
                .authorName(authorName)
                .authorEmail(authorEmail)
                .authorId(authorId)           // ✅ ДОБАВЛЕНО
                .authorImageUrl(autorImageUrl) // ✅ ДОБАВЛЕНО
                .build();
    }

    public ForumPageCardDTO toPageCardDTO(Forum forum) {
        if (forum == null) {
            return null;
        }

        // ✅ ИСПРАВЛЕНО: правильная логика имени
        String authorName = "Неизвестный автор";
        if (forum.getUser() != null) {
            User user = forum.getUser();
            if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
                authorName = user.getFirstName(); // ✅ ИСПРАВЛЕНО: было getLastName()
                if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
                    authorName += " " + user.getLastName();
                }
            } else {
                authorName = user.getEmail();
            }
        }

        return ForumPageCardDTO.builder()
                .id(forum.getId())
                .title(forum.getTitle())
                .forumType(forum.getForumType())
                .viewCount(forum.getViewCount())
                .commentCount(forum.getCommentCount())
                .createdAt(forum.getCreatedAt())
                .authorName(authorName)
                .build();
    }

    public ForumPageResponseDTO toPageResponseDTO(Page<Forum> page) {
        if (page == null) {
            return ForumPageResponseDTO.builder()
                    .content(List.of())
                    .page(0)
                    .size(0)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .sortBy("createdAt")
                    .sortDirection("DESC")
                    .build();
        }

        List<ForumPageCardDTO> content = page.getContent()
                .stream()
                .map(this::toPageCardDTO)
                .collect(Collectors.toList());

        Sort.Order order = page.getSort().iterator().hasNext()
                ? page.getSort().iterator().next()
                : null;

        return ForumPageResponseDTO.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .sortBy(order != null ? order.getProperty() : "createdAt")
                .sortDirection(order != null ? order.getDirection().name() : "DESC")
                .build();
    }
}