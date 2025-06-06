package com.example.cleopatra.maper;

import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserResponse;
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
        ///подпищики
        // Устанавливаем значения по умолчанию, если в User они null
        response.setFollowersCount(user.getFollowersCount() != null ? user.getFollowersCount() : 0L);
        response.setFollowingCount(user.getFollowingCount() != null ? user.getFollowingCount() : 0L);


        response.setCity(user.getCity());

        response.setCreatedAt(user.getCreatedAt());

        response.setIsOnline(user.getIsOnline());
        response.setLastSeen(user.getLastSeen());
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
    }

}
