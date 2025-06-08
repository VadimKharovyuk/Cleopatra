package com.example.cleopatra.maper;

import com.example.cleopatra.dto.Comment.CommentPageResponse;
import com.example.cleopatra.dto.Comment.CommentResponse;
import com.example.cleopatra.model.Comment;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentMapper {

    /**
     * Преобразует Comment entity в CommentResponse DTO
     */
    public CommentResponse toCommentResponse(Comment comment) {
        if (comment == null) {
            return null;
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .author(toAuthorDto(comment))
                .build();
    }

    /**
     * Преобразует информацию об авторе
     */
    private CommentResponse.CommentAuthorDto toAuthorDto(Comment comment) {
        if (comment.getAuthor() == null) {
            return null;
        }

        return CommentResponse.CommentAuthorDto.builder()
                .id(comment.getAuthor().getId())
                .firstName(comment.getAuthor().getFirstName())
                .lastName(comment.getAuthor().getLastName())
                .imageUrl(comment.getAuthor().getImageUrl())
                .build();
    }

    /**
     * Преобразует Slice<Comment> в CommentPageResponse с пагинацией
     */
    public CommentPageResponse toCommentPageResponse(Slice<Comment> commentSlice, long totalElements) {
        List<CommentResponse> commentResponses = commentSlice.getContent()
                .stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        CommentPageResponse.PageInfo pageInfo = CommentPageResponse.PageInfo.builder()
                .currentPage(commentSlice.getNumber())
                .pageSize(commentSlice.getSize())
                .hasNext(commentSlice.hasNext())
                .hasPrevious(commentSlice.hasPrevious())
                .totalElements(totalElements)
                .isFirst(commentSlice.isFirst())
                .isLast(commentSlice.isLast())
                .build();

        return CommentPageResponse.builder()
                .comments(commentResponses)
                .pageInfo(pageInfo)
                .build();
    }

    /**
     * Преобразует список комментариев в список DTO
     */
    public List<CommentResponse> toCommentResponseList(List<Comment> comments) {
        return comments.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }
}