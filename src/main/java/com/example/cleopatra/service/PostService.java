package com.example.cleopatra.service;

import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.Post.PostResponseDto;

public interface PostService {


    PostResponseDto createPost(PostCreateDto postCreateDto);
    PostResponseDto getPostById(Long id);

  PostListDto getUserPosts(Long userId, int page, int size) ;


    PostListDto getMyPosts(int page, int size);

    /**
     * Получить ленту новостей (посты подписок) для пользователя
     */
    PostListDto getFeedPosts(Long userId, int page, int size);

    /**
     * Получить рекомендованные посты если нет подписок
     */
    PostListDto getRecommendedPosts(Long userId, int page, int size);
}
