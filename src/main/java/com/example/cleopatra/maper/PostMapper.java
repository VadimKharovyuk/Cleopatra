package com.example.cleopatra.maper;

import com.example.cleopatra.dto.Post.PostCardDto;
import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostMapper {

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
     * ✅ УПРОЩЕННЫЙ - принимает готовые данные о лайках
     */
    public PostResponseDto toResponseDto(Post post, Boolean isLikedByCurrentUser,
                                         List<PostResponseDto.LikeUserDto> recentLikes) {
        return PostResponseDto.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .author(toAuthorDto(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .viewsCount(post.getViewsCount())
                .isLikedByCurrentUser(isLikedByCurrentUser)
                .recentLikes(recentLikes)
                .build();
    }

    /**
     * ✅ УПРОЩЕННЫЙ - принимает готовые данные о лайках
     */
    public PostCardDto toCardDto(Post post, Boolean isLikedByCurrentUser,
                                 List<PostCardDto.LikeUserDto> recentLikes) {
        String originalContent = post.getContent();
        boolean isLongContent = originalContent.length() > CONTENT_PREVIEW_LENGTH;
        String previewContent = isLongContent
                ? originalContent.substring(0, CONTENT_PREVIEW_LENGTH) + "..."
                : originalContent;

        return PostCardDto.builder()
                .id(post.getId())
                .content(previewContent)
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

    // ✅ УТИЛИТНЫЕ МЕТОДЫ для конвертации User в DTO

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