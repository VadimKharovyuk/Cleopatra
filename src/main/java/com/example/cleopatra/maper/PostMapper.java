//package com.example.cleopatra.maper;
//import com.example.cleopatra.dto.Location.LocationDto;
//import com.example.cleopatra.dto.Post.PostCardDto;
//import com.example.cleopatra.dto.Post.PostCreateDto;
//import com.example.cleopatra.dto.Post.PostListDto;
//import com.example.cleopatra.dto.Post.PostResponseDto;
//import com.example.cleopatra.model.Post;
//import com.example.cleopatra.model.User;
//import com.example.cleopatra.service.MentionService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//@RequiredArgsConstructor
//@Component
//public class PostMapper {
//    private final MentionService mentionService;
//
//    private static final int CONTENT_PREVIEW_LENGTH = 200;
//
//    /**
//     * Преобразует PostCreateDto в Post entity
//     */
//    public Post toEntity(PostCreateDto dto, User author) {
//        return Post.builder()
//                .content(dto.getContent())
//                .author(author)
//                .likesCount(0L)
//                .commentsCount(0L)
//                .viewsCount(0L)
//                .isDeleted(false)
//                .build();
//    }
//
//    /**
//     * ✅ ИСПРАВЛЕНО - убрана дублированная строка location
//     */
//    public PostResponseDto toResponseDto(Post post, Boolean isLikedByCurrentUser,
//                                         List<PostResponseDto.LikeUserDto> recentLikes) {
//
//
//        String contentWithLinks = mentionService.convertMentionsToLinksWithCache(
//                post.getContent(),
//                post.getId()
//        );
//        return PostResponseDto.builder()
//                .id(post.getId())
//                .content(contentWithLinks)
//                .imageUrl(post.getImageUrl())
//                .author(toAuthorDto(post.getAuthor()))
//                .createdAt(post.getCreatedAt())
//                .likesCount(post.getLikesCount())
//                .commentsCount(post.getCommentsCount())
//                .viewsCount(post.getViewsCount())
//                .isLikedByCurrentUser(isLikedByCurrentUser)
//                .recentLikes(recentLikes)
//                .location(post.getLocation() != null ? LocationDto.from(post.getLocation()) : null) // ✅ ОСТАВЛЯЕМ ТОЛЬКО ОДНУ СТРОКУ
//                .build();
//    }
//
//    /**
//     * ✅ ДОБАВЛЕНА ГЕОЛОКАЦИЯ В КАРТОЧКУ ПОСТА
//     */
//    public PostCardDto toCardDto(Post post, Boolean isLikedByCurrentUser,
//                                 List<PostCardDto.LikeUserDto> recentLikes) {
//        String originalContent = post.getContent();
//        boolean isLongContent = originalContent.length() > CONTENT_PREVIEW_LENGTH;
//        String previewContent = isLongContent
//                ? originalContent.substring(0, CONTENT_PREVIEW_LENGTH) + "..."
//                : originalContent;
//
//        return PostCardDto.builder()
//                .id(post.getId())
//                .content(previewContent)
//                .imageUrl(post.getImageUrl())
//                .author(toCardAuthorDto(post.getAuthor()))
//                .createdAt(post.getCreatedAt())
//                .likesCount(post.getLikesCount())
//                .commentsCount(post.getCommentsCount())
//                .viewsCount(post.getViewsCount())
//                .hasImage(post.getImageUrl() != null && !post.getImageUrl().isEmpty())
//                .isLongContent(isLongContent)
//                .isLikedByCurrentUser(isLikedByCurrentUser)
//                .recentLikes(recentLikes)
//
//                // ✅ ДОБАВЛЯЕМ ГЕОЛОКАЦИЮ
//                .location(post.getLocation() != null ? LocationDto.from(post.getLocation()) : null)
//                .hasLocation(post.getLocation() != null) // Можно убрать, так как есть метод hasLocation()
//
//                .build();
//    }
//
//    /**
//     * Создает PostListDto из готовых данных
//     */
//    public PostListDto toListDto(List<PostCardDto> postCards,
//                                 int currentPage,
//                                 boolean hasNext,
//                                 int pageSize) {
//        return PostListDto.builder()
//                .posts(postCards)
//                .currentPage(currentPage)
//                .pageSize(pageSize)
//                .hasNext(hasNext)
//                .hasPrevious(currentPage > 0)
//                .nextPage(hasNext ? currentPage + 1 : null)
//                .previousPage(currentPage > 0 ? currentPage - 1 : null)
//                .isEmpty(postCards.isEmpty())
//                .numberOfElements(postCards.size())
//                .build();
//    }
//
//    // ✅ УТИЛИТНЫЕ МЕТОДЫ для конвертации User в DTO
//
//    /**
//     * Преобразует User в LikeUserDto для PostResponseDto
//     */
//    public PostResponseDto.LikeUserDto toLikeUserDto(User user) {
//        return PostResponseDto.LikeUserDto.builder()
//                .id(user.getId())
//                .firstName(user.getFirstName())
//                .lastName(user.getLastName())
//                .imageUrl(user.getImageUrl())
//                .build();
//    }
//
//    /**
//     * Преобразует User в LikeUserDto для PostCardDto
//     */
//    public PostCardDto.LikeUserDto toCardLikeUserDto(User user) {
//        return PostCardDto.LikeUserDto.builder()
//                .id(user.getId())
//                .firstName(user.getFirstName())
//                .lastName(user.getLastName())
//                .imageUrl(user.getImageUrl())
//                .build();
//    }
//
//    /**
//     * Преобразует User в AuthorDto для PostResponseDto
//     */
//    private PostResponseDto.AuthorDto toAuthorDto(User user) {
//        return PostResponseDto.AuthorDto.builder()
//                .id(user.getId())
//                .firstName(user.getFirstName())
//                .lastName(user.getLastName())
//                .imageUrl(user.getImageUrl())
//                .build();
//    }
//
//    /**
//     * Преобразует User в AuthorDto для PostCardDto
//     */
//    private PostCardDto.AuthorDto toCardAuthorDto(User user) {
//        return PostCardDto.AuthorDto.builder()
//                .id(user.getId())
//                .firstName(user.getFirstName())
//                .lastName(user.getLastName())
//                .imageUrl(user.getImageUrl())
//                .build();
//    }
//}

package com.example.cleopatra.maper;

import com.example.cleopatra.dto.Location.LocationDto;
import com.example.cleopatra.dto.Post.PostCardDto;
import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.MentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class PostMapper {

    private final MentionService mentionService;
    private static final int CONTENT_PREVIEW_LENGTH = 200;

    /**
     * Преобразует PostCreateDto в Post entity
     */
    public Post toEntity(PostCreateDto dto, User author) {
        return Post.builder()
                .content(dto.getContent())
                .author(author)
                .likesCount(0L)
                .commentsCount(0L)
                .viewsCount(0L)
                .isDeleted(false)
                .build();
    }

    /**
     * ✅ ОБНОВЛЕНО - добавлена обработка упоминаний
     */
    public PostResponseDto toResponseDto(Post post, Boolean isLikedByCurrentUser,
                                         List<PostResponseDto.LikeUserDto> recentLikes) {

        // ✅ Обрабатываем упоминания в полном контенте
        String contentWithLinks = processContentMentions(post.getContent(), post.getId());

        return PostResponseDto.builder()
                .id(post.getId())
                .content(contentWithLinks) // ✅ Контент с кликабельными ссылками
                .imageUrl(post.getImageUrl())
                .author(toAuthorDto(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .viewsCount(post.getViewsCount())
                .isLikedByCurrentUser(isLikedByCurrentUser)
                .recentLikes(recentLikes)
                .location(post.getLocation() != null ? LocationDto.from(post.getLocation()) : null)
                .build();
    }

    /**
     * ✅ ОБНОВЛЕНО - добавлена обработка упоминаний в превью
     */
    public PostCardDto toCardDto(Post post, Boolean isLikedByCurrentUser,
                                 List<PostCardDto.LikeUserDto> recentLikes) {

        String originalContent = post.getContent();
        boolean isLongContent = originalContent.length() > CONTENT_PREVIEW_LENGTH;

        // ✅ Создаем превью контента
        String previewContent = isLongContent
                ? originalContent.substring(0, CONTENT_PREVIEW_LENGTH) + "..."
                : originalContent;

        // ✅ Обрабатываем упоминания в превью контенте
        String contentWithLinks = processContentMentions(previewContent, post.getId());

        return PostCardDto.builder()
                .id(post.getId())
                .content(contentWithLinks) // ✅ Превью контент с кликабельными ссылками
                .imageUrl(post.getImageUrl())
                .author(toCardAuthorDto(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .viewsCount(post.getViewsCount())
                .hasImage(post.getImageUrl() != null && !post.getImageUrl().isEmpty())
                .isLongContent(isLongContent)
                .isLikedByCurrentUser(isLikedByCurrentUser)
                .recentLikes(recentLikes)
                .location(post.getLocation() != null ? LocationDto.from(post.getLocation()) : null)
                .hasLocation(post.getLocation() != null)
                .build();
    }

    /**
     * ✅ НОВЫЙ МЕТОД - версия без обработки упоминаний для производительности
     */
    public PostCardDto toCardDtoSimple(Post post, Boolean isLikedByCurrentUser,
                                       List<PostCardDto.LikeUserDto> recentLikes) {

        String originalContent = post.getContent();
        boolean isLongContent = originalContent.length() > CONTENT_PREVIEW_LENGTH;
        String previewContent = isLongContent
                ? originalContent.substring(0, CONTENT_PREVIEW_LENGTH) + "..."
                : originalContent;

        return PostCardDto.builder()
                .id(post.getId())
                .content(previewContent) // ✅ Без обработки упоминаний
                .imageUrl(post.getImageUrl())
                .author(toCardAuthorDto(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .viewsCount(post.getViewsCount())
                .hasImage(post.getImageUrl() != null && !post.getImageUrl().isEmpty())
                .isLongContent(isLongContent)
                .isLikedByCurrentUser(isLikedByCurrentUser)
                .recentLikes(recentLikes)
                .location(post.getLocation() != null ? LocationDto.from(post.getLocation()) : null)
                .hasLocation(post.getLocation() != null)
                .build();
    }

    /**
     * Создает PostListDto из готовых данных
     */
    public PostListDto toListDto(List<PostCardDto> postCards,
                                 int currentPage,
                                 boolean hasNext,
                                 int pageSize) {
        return PostListDto.builder()
                .posts(postCards)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .hasNext(hasNext)
                .hasPrevious(currentPage > 0)
                .nextPage(hasNext ? currentPage + 1 : null)
                .previousPage(currentPage > 0 ? currentPage - 1 : null)
                .isEmpty(postCards.isEmpty())
                .numberOfElements(postCards.size())
                .build();
    }

    // ===== ✅ НОВЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    /**
     * ✅ Безопасная обработка упоминаний в контенте
     */
    private String processContentMentions(String content, Long postId) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        try {
            return mentionService.convertMentionsToLinksWithCache(content, postId);
        } catch (Exception e) {
            log.warn("Ошибка обработки упоминаний для поста {}: {}", postId, e.getMessage());
            // Возвращаем оригинальный контент если обработка упоминаний не удалась
            return content;
        }
    }

    /**
     * ✅ Пакетная обработка упоминаний для списка постов
     */
    public List<PostCardDto> processCardMentionsInBatch(List<PostCardDto> postCards) {
        return postCards.stream()
                .peek(card -> {
                    try {
                        String contentWithLinks = mentionService.convertMentionsToLinksWithCache(
                                card.getContent(),
                                card.getId()
                        );
                        card.setContent(contentWithLinks);
                    } catch (Exception e) {
                        log.warn("Ошибка пакетной обработки упоминаний для поста {}: {}",
                                card.getId(), e.getMessage());
                        // Оставляем оригинальный контент
                    }
                })
                .toList();
    }

    // ===== СУЩЕСТВУЮЩИЕ УТИЛИТНЫЕ МЕТОДЫ (без изменений) =====

    /**
     * Преобразует User в LikeUserDto для PostResponseDto
     */
    public PostResponseDto.LikeUserDto toLikeUserDto(User user) {
        return PostResponseDto.LikeUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }

    /**
     * Преобразует User в LikeUserDto для PostCardDto
     */
    public PostCardDto.LikeUserDto toCardLikeUserDto(User user) {
        return PostCardDto.LikeUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }

    /**
     * Преобразует User в AuthorDto для PostResponseDto
     */
    private PostResponseDto.AuthorDto toAuthorDto(User user) {
        return PostResponseDto.AuthorDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }

    /**
     * Преобразует User в AuthorDto для PostCardDto
     */
    private PostCardDto.AuthorDto toCardAuthorDto(User user) {
        return PostCardDto.AuthorDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }
}