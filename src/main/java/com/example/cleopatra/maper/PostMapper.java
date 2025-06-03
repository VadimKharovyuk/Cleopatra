package com.example.cleopatra.maper;

import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

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
     * Преобразует Post entity в PostResponseDto
     */
    public PostResponseDto toResponseDto(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .author(toAuthorDto(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .viewsCount(post.getViewsCount())
                .build();
    }

    /**
     * Преобразует User в AuthorDto
     */
    private PostResponseDto.AuthorDto toAuthorDto(User user) {
        return PostResponseDto.AuthorDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }
}