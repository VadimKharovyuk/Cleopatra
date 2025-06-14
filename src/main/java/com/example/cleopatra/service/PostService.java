package com.example.cleopatra.service;

import com.example.cleopatra.dto.Post.*;
import com.example.cleopatra.model.Post;

import java.time.LocalDate;

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

    void deletePost(Long id);


    Post findById(Long postId);
    // ✅ НОВЫЕ МЕТОДЫ для лайков
    PostLikeResponseDto toggleLike(Long postId);
    PostLikeInfoDto getLikeInfo(Long postId);


    long getTotalPostsCount();


    long getPostsCountByDate(LocalDate today);


  long getPostsCountFromDate(LocalDate weekStart);


  long getPostsCountBetweenDates(LocalDate lastWeekStart, LocalDate lastWeekEnd);
}
