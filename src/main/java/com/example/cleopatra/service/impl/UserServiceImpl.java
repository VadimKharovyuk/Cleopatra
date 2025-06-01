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
import java.util.Random;

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

        UserResponse userResponse = userMapper.toResponse(user);
        Random random = new Random();
        userResponse.setFollowersCount((long) random.nextInt(1000) + 50);  // 50-1050
        userResponse.setFollowingCount((long) random.nextInt(500) + 20);   // 20-520
        userResponse.setPostsCount((long) random.nextInt(200) + 5);

//        userResponse.setFollowersCount(125L);  // количество подписчиков
//        userResponse.setFollowingCount(89L);   // количество подписок
//        userResponse.setPostsCount(47L);
        return userResponse;
    }
    // Вспомогательные методы для получения статистики
    private Long getFollowersCount(Long userId) {
        // TODO: когда будет готов Follow entity
        // return followRepository.countByFolloweeId(userId);

        // Временная заглушка для демонстрации
        return 125L; // можете поставить любое число для теста
    }

    private Long getFollowingCount(Long userId) {
        // TODO: когда будет готов Follow entity
        // return followRepository.countByFollowerId(userId);

        // Временная заглушка
        return 89L;
    }

    private Long getPostsCount(Long userId) {
        // TODO: когда будет готов Post entity
        // return postRepository.countByAuthorId(userId);

        // Временная заглушка
        return 47L;
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
    public UserResponse deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

        if (user.getImgId() != null && !user.getImgId().isEmpty()) {
            storageService.deleteImage(user.getImgId());
            user.setImageUrl(null);
            user.setImgId(null);
            User savedUser = userRepository.save(user);
            return userMapper.toResponse(savedUser);
        }

        return userMapper.toResponse(user);
    }
    @Override
    public UserResponse uploadBackgroundImage(Long userId, MultipartFile file) {
        log.info("Начинаем загрузку фонового изображения для пользователя с ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

            imageValidator.validateImage(file);

            if (user.getImgBackgroundID() != null && !user.getImgBackgroundID().isEmpty()) {
                log.debug("Удаляем старое фоновое изображение с ID: {}", user.getImgBackgroundID());
                storageService.deleteImage(user.getImgBackgroundID());
            }

            StorageService.StorageResult uploadResult = storageService.uploadImage(file);

            user.setImgBackground(uploadResult.getUrl());
            user.setImgBackgroundID(uploadResult.getImageId());

            User savedUser = userRepository.save(user);

            log.info("Фоновое изображение успешно загружено для пользователя с ID: {}. URL: {}",
                    userId, uploadResult.getUrl());

            return userMapper.toResponse(savedUser);

        } catch (IOException e) {

            log.error("Ошибка при загрузке фонового изображения для пользователя с ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при загрузке фонового изображения: " + e.getMessage(), e);
        }
    }
    @Override
    public UserResponse deleteBackgroundImage(Long userId) {
        log.info("Удаляем фоновое изображение для пользователя с ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

        if (user.getImgBackgroundID() != null && !user.getImgBackgroundID().isEmpty()) {
            try {
                storageService.deleteImage(user.getImgBackgroundID());

                user.setImgBackground(null);
                user.setImgBackgroundID(null);

                User savedUser = userRepository.save(user);

                log.info("Фоновое изображение успешно удалено для пользователя с ID: {}", userId);
                return userMapper.toResponse(savedUser);

            } catch (Exception e) {
                log.error("Ошибка при удалении фонового изображения для пользователя с ID {}: {}", userId, e.getMessage());
                throw new RuntimeException("Ошибка при удалении фонового изображения: " + e.getMessage(), e);
            }
        } else {
            log.debug("У пользователя с ID {} нет фонового изображения для удаления", userId);
            return userMapper.toResponse(user);
        }

    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return userMapper.toResponse(user);
    }


}
