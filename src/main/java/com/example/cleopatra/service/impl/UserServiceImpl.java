package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.UserAlreadyExistsException;
import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.maper.UserMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.SubscriptionRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.ImageConverterService;
import com.example.cleopatra.service.ImageValidator;
import com.example.cleopatra.service.StorageService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final SubscriptionRepository subscriptionRepository;
    private final PostRepository postRepository;


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

            // ✅ ИЗМЕНЕНИЕ: Используем validateAndProcess вместо validateImage
            ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(file);

            // Логируем информацию о конвертации
            if (!processedImage.getContentType().equals(file.getContentType())) {
                log.info("Файл {} был конвертирован из {} в {}",
                        file.getOriginalFilename(),
                        file.getContentType(),
                        processedImage.getContentType());
            }

            if (user.getImgId() != null && !user.getImgId().isEmpty()) {
                log.debug("Удаляем старый аватар с ID: {}", user.getImgId());
                storageService.deleteImage(user.getImgId());
            }

            // ✅ ИЗМЕНЕНИЕ: Передаем обработанное изображение в storage
            StorageService.StorageResult uploadResult = storageService.uploadProcessedImage(processedImage);

            user.setImageUrl(uploadResult.getUrl());
            user.setImgId(uploadResult.getImageId());

            User savedUser = userRepository.save(user);

            log.info("Аватар успешно загружен для пользователя с ID: {}. URL: {}, финальный формат: {}",
                    userId, uploadResult.getUrl(), processedImage.getContentType());

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
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        // Маппим базовую информацию
        UserResponse userResponse = userMapper.toResponse(user);

        // Добавляем статистику
        userResponse.setPostsCount(getPostsCount(userId));
        userResponse.setFollowersCount(getFollowersCount(userId));
        userResponse.setFollowingCount(getFollowingCount(userId));

        return userResponse;
    }


    private Long getFollowersCount(Long userId) {
        return subscriptionRepository.countBySubscriberId(userId);

    }

    private Long getFollowingCount(Long userId) {
      return   subscriptionRepository.countBySubscriberId(userId);
    }

    private Long getPostsCount(Long userId) {
        Long count = postRepository.countByAuthorId(userId);
        log.info("🔢 Подсчет постов для пользователя {}: {}", userId, count);
        return count;
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

            // ✅ ИЗМЕНЕНИЕ: Используем validateAndProcess вместо validateImage
            ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(file);

            // Логируем информацию о конвертации
            if (!processedImage.getContentType().equals(file.getContentType())) {
                log.info("Фоновое изображение {} было конвертировано из {} в {}",
                        file.getOriginalFilename(),
                        file.getContentType(),
                        processedImage.getContentType());
            }

            if (user.getImgBackgroundID() != null && !user.getImgBackgroundID().isEmpty()) {
                log.debug("Удаляем старое фоновое изображение с ID: {}", user.getImgBackgroundID());
                storageService.deleteImage(user.getImgBackgroundID());
            }

            // ✅ ИЗМЕНЕНИЕ: Передаем обработанное изображение в storage
            StorageService.StorageResult uploadResult = storageService.uploadProcessedImage(processedImage);

            user.setImgBackground(uploadResult.getUrl());
            user.setImgBackgroundID(uploadResult.getImageId());

            User savedUser = userRepository.save(user);

            log.info("Фоновое изображение успешно загружено для пользователя с ID: {}. URL: {}, финальный формат: {}",
                    userId, uploadResult.getUrl(), processedImage.getContentType());

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

    @Override
    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }


}
