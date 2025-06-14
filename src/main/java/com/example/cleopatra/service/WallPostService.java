package com.example.cleopatra.service;

import com.example.cleopatra.dto.WallPost.*;
import com.example.cleopatra.enums.WallAccessLevel;

import java.io.IOException;

public interface WallPostService {

    WallPostCardResponse create(WallPostCreateRequest request, Long currentUserId) throws IOException;

    WallPostPageResponse getWallPosts(Long wallOwnerId, Long currentUserId, int page, int size);
    WallPostCardResponse getById(Long id, Long currentUserId);


    // Удаление поста
    void delete(Long postId, Long currentUserId);
    // Проверка доступа к стене
    boolean canAccessWall(Long wallOwnerId, Long visitorId);
}
