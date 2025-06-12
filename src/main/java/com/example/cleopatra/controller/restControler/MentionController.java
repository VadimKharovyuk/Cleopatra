package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.UserMention.MentionDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.PostMention;
import com.example.cleopatra.service.MentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.MentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class MentionController {

    private final MentionService mentionService;



//    @GetMapping("/mentions/search")
//    public ResponseEntity<List<MentionDto.UserMentionDto>> searchUsers(@RequestParam String query) {
//        List<User> users = mentionService.searchUsersForMentions(query);
//
//        List<MentionDto.UserMentionDto> userDtos = users.stream()
//                .map(this::convertToMentionDto)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(userDtos);
//    }


    /**
     * Поиск пользователей для автодополнения упоминаний
     * GET /api/mentions/search?query=имя
     */
    @GetMapping("/mentions/search")
    public ResponseEntity<List<MentionDto.UserMentionDto>> searchUsers(@RequestParam String query) {
        log.info("Поиск пользователей для упоминаний: '{}'", query);

        try {
            // Валидация запроса
            if (query == null || query.trim().isEmpty()) {
                log.warn("Слишком короткий запрос для поиска: '{}'", query);
                return ResponseEntity.ok(List.of());
            }

            if (query.length() > 100) {
                log.warn("Слишком длинный запрос для поиска: '{}'", query);
                return ResponseEntity.badRequest().build();
            }

            // Поиск пользователей
            List<User> users = mentionService.searchUsersForMentions(query);

            // Конвертация в DTO
            List<MentionDto.UserMentionDto> userDtos = users.stream()
                    .map(this::convertToMentionDto)
                    .collect(Collectors.toList());

            log.info("Найдено {} пользователей для запроса: '{}'", userDtos.size(), query);
            return ResponseEntity.ok(userDtos);

        } catch (Exception e) {
            log.error("Ошибка при поиске пользователей для запроса '{}': {}", query, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }




    /**
     * Получить упоминания текущего пользователя
     * GET /api/mentions/my
     */
    @GetMapping("/mentions/my")
    public ResponseEntity<List<MentionDto>> getMyMentions() {
        try {
            // TODO: Получить текущего пользователя из SecurityContext
            // User currentUser = userService.getCurrentUser();
            // List<PostMention> mentions = mentionService.getUserMentions(currentUser);

            // Пока возвращаем пустой список
            return ResponseEntity.ok(List.of());

        } catch (Exception e) {
            log.error("Ошибка при получении упоминаний пользователя: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить упоминания для конкретного поста
     * GET /api/mentions/post/{postId}
     */
    @GetMapping("/mentions/post/{postId}")
    public ResponseEntity<List<MentionDto>> getPostMentions(@PathVariable Long postId) {
        try {
            List<PostMention> mentions = mentionService.getPostMentions(postId);

            List<MentionDto> mentionDtos = mentions.stream()
                    .map(this::convertToMentionDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(mentionDtos);

        } catch (Exception e) {
            log.error("Ошибка при получении упоминаний для поста {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Конвертация User в UserMentionDto
     */
    private MentionDto.UserMentionDto convertToMentionDto(User user) {
        return MentionDto.UserMentionDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl()) // если есть поле imageUrl в User
                .build();
    }

    /**
     * Конвертация PostMention в MentionDto
     */
    private MentionDto convertToMentionDto(PostMention mention) {
        return MentionDto.builder()
                .id(mention.getId())
                .postId(mention.getPost().getId())
                .mentionedUser(convertToUserDto(mention.getMentionedUser()))
                .mentionedBy(convertToUserDto(mention.getMentionedBy()))
                .createdAt(mention.getCreatedAt())
                .build();
    }

    private MentionDto.UserMentionDto convertToUserDto(User user) {
        return MentionDto.UserMentionDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }
}
