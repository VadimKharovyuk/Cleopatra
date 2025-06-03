package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.maper.PostMapper;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final StorageService storageService;
    private final ImageValidator imageValidator;
    private final PostMapper postMapper;
    private final UserRepository userRepository;

    @Override
    public PostResponseDto createPost(PostCreateDto postCreateDto) {
        log.info("Создание нового поста");

        // Получаем текущего пользователя
        User currentUser = getCurrentUser();

        // Создаем пост через маппер
        Post post = postMapper.toEntity(postCreateDto, currentUser);

        // Обрабатываем изображение, если оно есть
        if (postCreateDto.getImage() != null && !postCreateDto.getImage().isEmpty()) {
            try {
                // Валидация и обработка изображения
                ImageConverterService.ProcessedImage processedImage =
                        imageValidator.validateAndProcess(postCreateDto.getImage());

                // Загрузка в хранилище
                StorageService.StorageResult storageResult =
                        storageService.uploadProcessedImage(processedImage);

                // Устанавливаем URL и ID изображения
                post.setImageUrl(storageResult.getUrl());
                post.setImgId(storageResult.getImageId());

                log.info("Изображение успешно загружено: {}", storageResult.getImageId());

            } catch (Exception e) {
                log.error("Ошибка при загрузке изображения: {}", e.getMessage());
                throw new RuntimeException("Не удалось загрузить изображение: " + e.getMessage());
            }
        }

        // Сохраняем пост
        Post savedPost = postRepository.save(post);

        // Увеличиваем счетчик постов у пользователя (с проверкой на null)
        Long currentPostsCount = currentUser.getPostsCount();
        if (currentPostsCount == null) {
            currentUser.setPostsCount(1L);
        } else {
            currentUser.setPostsCount(currentPostsCount + 1);
        }

        // Сохраняем изменения пользователя
        userRepository.save(currentUser);

        log.info("Пост успешно создан с ID: {}", savedPost.getId());

        return postMapper.toResponseDto(savedPost);
    }

    private User getCurrentUser() {
        return userService.getCurrentUserEntity();
    }
}
