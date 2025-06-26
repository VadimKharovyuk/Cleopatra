package com.example.cleopatra.maper;

import com.example.cleopatra.dto.GroupDto.CreateGroupPostRequest;
import com.example.cleopatra.dto.GroupDto.GroupPostResponse;
import com.example.cleopatra.dto.GroupDto.GroupPostDetails;
import com.example.cleopatra.dto.GroupDto.GroupPostsSliceResponse;
import com.example.cleopatra.model.GroupPost;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GroupPostMapper {

    public GroupPost toEntity(CreateGroupPostRequest request) {
        if (request == null) {
            return null;
        }

        GroupPost groupPost = new GroupPost();
        groupPost.setText(request.getText());
        return groupPost;
    }

    /**
     * Преобразование GroupPost Entity в GroupPostResponse
     */
    public GroupPostResponse toResponse(GroupPost post) {
        if (post == null) {
            return null;
        }

        GroupPostResponse response = new GroupPostResponse();
        response.setId(post.getId());
        response.setText(post.getText());
        response.setImageUrl(post.getImageUrl());
        response.setImgId(post.getImgId());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());

        // Устанавливаем hasImage
        response.setHasImage(post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty());

        // Устанавливаем данные группы
        if (post.getGroup() != null) {
            response.setGroupId(post.getGroup().getId());
        } else {
            response.setGroupId(post.getGroupId());
        }

        // Устанавливаем данные автора
        if (post.getAuthor() != null) {
            response.setAuthorId(post.getAuthor().getId());
            response.setAuthorName(post.getAuthor().getFirstName());
            response.setAuthorImageUrl(post.getAuthor().getImageUrl());
        } else {
            response.setAuthorId(post.getAuthorId());

        }

        return response;
    }


    /**
     * Обновленный метод toDetails в GroupPostMapper
     */
    public GroupPostDetails toDetails(GroupPost post) {
        if (post == null) {
            return null;
        }

        GroupPostDetails details = new GroupPostDetails();
        details.setId(post.getId());
        details.setText(post.getText());
        details.setImageUrl(post.getImageUrl());
        details.setCreatedAt(post.getCreatedAt());
        details.setLikeCount(post.getLikeCount());
        details.setCommentCount(post.getCommentCount());
        details.setHasImage(post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty());
        details.setLikedByCurrentUser(false);

        // Данные автора
        if (post.getAuthor() != null) {
            details.setAuthorId(post.getAuthor().getId());
            details.setAuthorName(post.getAuthor().getFirstName()); // ← Используем firstName
            details.setAuthorImageUrl(post.getAuthor().getImageUrl());
        } else {
            details.setAuthorId(post.getAuthorId());

        }

        return details;
    }

    /**
     * Преобразование списка GroupPost в список GroupPostDetails
     */
    public List<GroupPostDetails> toDetailsList(List<GroupPost> posts) {
        if (posts == null) {
            return null;
        }

        return posts.stream()
                .map(this::toDetails)
                .collect(Collectors.toList());
    }


    public GroupPostsSliceResponse fromDetailsSlice(Slice<GroupPostDetails> slice) {
        if (slice == null) {
            return null;
        }

        GroupPostsSliceResponse response = new GroupPostsSliceResponse();
        response.setPosts(slice.getContent());
        response.setHasNext(slice.hasNext());
        response.setHasPrevious(slice.hasPrevious());
        response.setCurrentPage(slice.getNumber());
        response.setSize(slice.getSize());
        response.setFirst(slice.isFirst());
        response.setLast(slice.isLast());

        return response;
    }

}