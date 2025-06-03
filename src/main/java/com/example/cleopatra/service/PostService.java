package com.example.cleopatra.service;

import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.Post.PostResponseDto;

public interface PostService {


    PostResponseDto createPost(PostCreateDto postCreateDto);
    PostResponseDto getPostById(Long id);

  PostListDto getUserPosts(Long userId, int page, int size) ;


    PostListDto getMyPosts(int page, int size);
}
