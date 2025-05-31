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
        response.setImageUrl(user.getImageUrl());
        response.setImgId(user.getImgId());

        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public void updateUserFromDto(User user, UpdateProfileDto dto) {
        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName().trim());
        }

        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName().trim());
        }

    }

}
