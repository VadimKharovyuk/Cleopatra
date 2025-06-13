package com.example.cleopatra.maper;

import org.springframework.stereotype.Component;


import com.example.cleopatra.dto.WallPost.UserBriefResponse;
import com.example.cleopatra.dto.WallPost.WallPostCardResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.model.WallPost;
import org.springframework.stereotype.Component;

@Component
public class WallPostMapper {

    /**
     * Конвертирует WallPost в WallPostCardResponse
     * @param wallPost пост для конвертации
     * @param currentUserId ID текущего пользователя (для проверки прав)
     * @return WallPostCardResponse
     */
    public WallPostCardResponse toCardResponse(WallPost wallPost, Long currentUserId) {
        if (wallPost == null) {
            return null;
        }

        return WallPostCardResponse.builder()
                .id(wallPost.getId())
                .text(wallPost.getText())
                .picUrl(wallPost.getPicUrl())
                .author(toUserBriefResponse(wallPost.getAuthor()))
                .wallOwner(toUserBriefResponse(wallPost.getWallOwner()))
                .likesCount(wallPost.getLikesCount())
                .commentsCount(wallPost.getCommentsCount())
                .createdAt(wallPost.getCreatedAt())
                .canEdit(canEditPost(wallPost, currentUserId))
                .canDelete(canDeletePost(wallPost, currentUserId))
                .build();
    }

    /**
     * Конвертирует User в UserBriefResponse
     * @param user пользователь для конвертации
     * @return UserBriefResponse
     */
    public UserBriefResponse toUserBriefResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserBriefResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .build();
    }

    /**
     * Проверяет, может ли пользователь редактировать пост
     * @param wallPost пост
     * @param currentUserId ID текущего пользователя
     * @return true если может редактировать
     */
    private boolean canEditPost(WallPost wallPost, Long currentUserId) {
        if (currentUserId == null || wallPost.getAuthor() == null) {
            return false;
        }
        // Редактировать может только автор поста
        return wallPost.getAuthor().getId().equals(currentUserId);
    }

    /**
     * Проверяет, может ли пользователь удалить пост
     * @param wallPost пост
     * @param currentUserId ID текущего пользователя
     * @return true если может удалить
     */
    private boolean canDeletePost(WallPost wallPost, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }

        // Удалить может автор поста или владелец стены
        boolean isAuthor = wallPost.getAuthor() != null &&
                wallPost.getAuthor().getId().equals(currentUserId);
        boolean isWallOwner = wallPost.getWallOwner() != null &&
                wallPost.getWallOwner().getId().equals(currentUserId);

        return isAuthor || isWallOwner;
    }
}