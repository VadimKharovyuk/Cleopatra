package com.example.cleopatra.service;

import com.example.cleopatra.dto.GroupDto.CreateGroupPostRequest;
import com.example.cleopatra.dto.GroupDto.GroupPostResponse;
import com.example.cleopatra.dto.GroupDto.GroupPostsSliceResponse;
import com.example.cleopatra.dto.GroupDto.UpdateGroupPostRequest;
import com.example.cleopatra.model.GroupPost;


public interface GroupPostService {
    GroupPostResponse createPost(CreateGroupPostRequest request, Long authorId) ;

    void deletePost(Long postId, Long authorId);
    GroupPostResponse updatePost(Long postId, UpdateGroupPostRequest request, Long authorId);

    GroupPostResponse getPostById(Long postId);

    GroupPostsSliceResponse getGroupPosts(Long groupId, Long currentUserId, int page, int size);
}
