package com.example.cleopatra.maper;

import com.example.cleopatra.dto.user.UserRecommendationDto;
import com.example.cleopatra.model.User;
import org.springframework.stereotype.Component;

@Component
public class RecommendationsMapper {


    public UserRecommendationDto mapToRecommendationDto(User user, Long currentUserId) {
        if (user == null) {
            return null;
        }

        UserRecommendationDto dto = new UserRecommendationDto();
        dto.setId(user.getId());
        dto.setImageUrl(user.getImageUrl());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFollowersCount((long) (user.getFollowersCount() != null ? user.getFollowersCount().intValue() : 0));

        dto.setIsFollowing(false); // TODO: реализовать когда будет Follow entity

        return dto;
    }
}
