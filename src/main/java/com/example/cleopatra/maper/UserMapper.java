package com.example.cleopatra.maper;

import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.ProfileAccessLevel;
import com.example.cleopatra.enums.Role;
import com.example.cleopatra.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterDto registerDto) {
        User user = new User();
        user.setEmail(registerDto.getEmail());
        user.setGender(registerDto.getGender());
        user.setRole(Role.USER);
        return user;
    }



    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setGender(user.getGender());

        // Добавляем новые поля профиля
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());

        ///аватар
        response.setImageUrl(user.getImageUrl());
        response.setImgId(user.getImgId());

        ///фон
        response.setImgBackground(user.getImgBackground());
        response.setImgBackgroundID(user.getImgBackgroundID());

        ///подписчики
        // Устанавливаем значения по умолчанию, если в User они null
        response.setFollowersCount(user.getFollowersCount() != null ? user.getFollowersCount() : 0L);
        response.setFollowingCount(user.getFollowingCount() != null ? user.getFollowingCount() : 0L);

        response.setCity(user.getCity());
        response.setCreatedAt(user.getCreatedAt());

        response.setIsOnline(user.getIsOnline());
        response.setLastSeen(user.getLastSeen());

        response.setReceiveVisitNotifications(user.getReceiveVisitNotifications());

        // СТАРОЕ ПОЛЕ - для совместимости (вычисляем из нового)
        response.setIsPrivateProfile(user.getProfileAccessLevel() == ProfileAccessLevel.PRIVATE);

        // НОВЫЕ ПОЛЯ ПРИВАТНОСТИ
        response.setProfileAccessLevel(user.getProfileAccessLevel());
        response.setPhotosAccessLevel(user.getPhotosAccessLevel());
        response.setPostsAccessLevel(user.getPostsAccessLevel());

        // БЛОКИРОВКА
        response.setIsBlocked(user.isBlocked());

        response.setBirthDate(user.getBirthDate());
        response.setShowBirthday(user.getShowBirthday());

        //статус
        response.setStatusPage(user.getStatusPage() != null ? user.getStatusPage() : "");

        ///онлайн // оффлайн статус
        response.setIsOnline(user.isOnline());
        response.setWasOnlineRecently(user.wasOnlineRecently());
        response.setLastSeen(user.getLastSeen());
        response.setDeviceType(user.getDeviceType());
        response.setOnlineStatusText(user.getOnlineStatusText());

        return response;
    }

    public void updateUserFromDto(User user, UpdateProfileDto dto) {
        // УЛУЧШИТЕ ПРОВЕРКИ:
        if (dto.getFirstName() != null && !dto.getFirstName().trim().isEmpty()) {
            user.setFirstName(dto.getFirstName().trim());
        }

        if (dto.getLastName() != null && !dto.getLastName().trim().isEmpty()) {
            user.setLastName(dto.getLastName().trim());
        }

        if (dto.getCity() != null && !dto.getCity().trim().isEmpty()) {
            user.setCity(dto.getCity().trim());
        } else if (dto.getCity() != null && dto.getCity().trim().isEmpty()) {
            // Если передали пустую строку, устанавливаем null
            user.setCity(null);
        }

        // Добавляем новые поля
        if (dto.getBirthDate() != null) {
            user.setBirthDate(dto.getBirthDate());
        }

        if (dto.getShowBirthday() != null) {
            user.setShowBirthday(dto.getShowBirthday());
        }

        // СТАТУС
        if (dto.getStatusPage() != null && !dto.getStatusPage().trim().isEmpty()) {
            user.setStatusPage(dto.getStatusPage().trim());
        } else if (dto.getStatusPage() != null && dto.getStatusPage().trim().isEmpty()) {
            // Если передали пустую строку, очищаем статус
            user.setStatusPage(null);
        }
    }

}
