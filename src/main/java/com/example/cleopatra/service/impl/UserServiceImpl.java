package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.UserAlreadyExistsException;
import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.maper.UserMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.ImageValidator;
import com.example.cleopatra.service.StorageService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ImageValidator imageValidator;
    private final StorageService storageService;


    @Override
    public UserResponse createUser(RegisterDto registerDto) {

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new UserAlreadyExistsException("Email уже используется");
        }
        User user = userMapper.toEntity(registerDto);
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse uploadAvatar(Long userId, MultipartFile file) {
        log.info("Начинаем загрузку аватара для пользователя с ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

            imageValidator.validateImage(file);

            if (user.getImgId() != null && !user.getImgId().isEmpty()) {
                log.debug("Удаляем старый аватар с ID: {}", user.getImgId());
                storageService.deleteImage(user.getImgId());
            }

            StorageService.StorageResult uploadResult = storageService.uploadImage(file);

            user.setImageUrl(uploadResult.getUrl());
            user.setImgId(uploadResult.getImageId());

            User savedUser = userRepository.save(user);

            log.info("Аватар успешно загружен для пользователя с ID: {}. URL: {}",
                    userId, uploadResult.getUrl());

            return userMapper.toResponse(savedUser);

        } catch (IOException e) {
            log.error("Ошибка при загрузке аватара для пользователя с ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при загрузке изображения: " + e.getMessage(), e);
        }
    }

    @Override
    public UserResponse updateProfile(Long userId, UpdateProfileDto profileDto) {
        log.info("Начинаем обновление профиля для пользователя с ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

            userMapper.updateUserFromDto(user, profileDto);

            User savedUser = userRepository.save(user);

            log.info("Профиль успешно обновлен для пользователя с ID: {}", userId);

            return userMapper.toResponse(savedUser);

        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении профиля для пользователя с ID {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));
        return userMapper.toResponse(user);
    }

        @Override
        public void validateUserExists(Long userId) {
            if (!userRepository.existsById(userId)) {
                throw new RuntimeException("Пользователь с ID " + userId + " не найден");
            }
    }

    @Override
    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public void deleteAvatar(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь с ID "+ userId + " не найден"));
        storageService.deleteImage(user.getImgId());
    }
}
