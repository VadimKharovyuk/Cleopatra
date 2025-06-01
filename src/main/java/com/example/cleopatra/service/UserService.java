package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse createUser(RegisterDto  registerDto);

    // Только загрузка/обновление аватара
 UserResponse uploadAvatar(Long userId, MultipartFile file) ;



  UserResponse updateProfile(Long userId, UpdateProfileDto profileDto);


    UserResponse getUserById(Long userId);

    void validateUserExists(Long userId);


    boolean userExists(Long userId);


    UserResponse deleteAvatar(Long userId);

    UserResponse uploadBackgroundImage(Long userId, MultipartFile file);
    UserResponse deleteBackgroundImage(Long userId);

    UserResponse getUserByEmail(String userEmail);


}
