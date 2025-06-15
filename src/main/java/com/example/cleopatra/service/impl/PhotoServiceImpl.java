package com.example.cleopatra.service.impl;
import com.example.cleopatra.ExistsException.ImageDeleteException;
import com.example.cleopatra.ExistsException.ImageUploadException;
import com.example.cleopatra.ExistsException.PhotoLimitExceededException;
import com.example.cleopatra.ExistsException.PhotoNotFoundException;
import com.example.cleopatra.dto.user.PhotoCreateDto;
import com.example.cleopatra.dto.user.PhotoResponseDto;
import com.example.cleopatra.maper.PhotoServiceMapper;
import com.example.cleopatra.model.Photo;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.PhotoRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.ImageValidator;
import com.example.cleopatra.service.PhotoService;
import com.example.cleopatra.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class PhotoServiceImpl implements PhotoService {

    private static final int DEFAULT_PHOTO_LIMIT = 10;
    private static final int ADMIN_PHOTO_LIMIT = Integer.MAX_VALUE;

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ImageValidator imageValidator ;
    private final PhotoServiceMapper photoServiceMapper ;
    private final PhotoRepository photoRepository;


    @Override
    public boolean canUploadPhoto(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UsernameNotFoundException("User not found: " + userId)
        );
        int limit = getPhotoLimitForUser(user);
        return user.getPhotoCount() < limit;
    }

    @Override
    public int getPhotoLimitForUser(User user) {
        return switch (user.getRole()) {
            case ADMIN -> ADMIN_PHOTO_LIMIT;
            default -> DEFAULT_PHOTO_LIMIT;
        };
    }

    @Override
    public PhotoResponseDto createPhoto(Long userId, PhotoCreateDto photoCreateDto) {
        // Проверяем лимит
        if (!canUploadPhoto(userId)) {
            User user = userRepository.findById(userId).orElseThrow();
            int limit = getPhotoLimitForUser(user);
            throw new PhotoLimitExceededException(
                    String.format("Достигнут лимит фотографий: %d/%d", user.getPhotoCount(), limit)
            );
        }

        // Валидируем изображение
        imageValidator.validateImage(photoCreateDto.getImage());

        try {
            // Загружаем в хранилище
            StorageService.StorageResult storageResult = storageService.uploadImage(photoCreateDto.getImage());
            String picUrl = storageResult.getUrl();
            String picId = generateUniqueId();

            // Создаем фото
            User user = userRepository.findById(userId).orElseThrow();
            Photo photo = Photo.builder()
                    .picUrl(picUrl)
                    .picId(picId)
                    .description(photoCreateDto.getDescription())
                    .author(user)
                    .uploadDate(LocalDateTime.now())
                    .build();

            photoRepository.save(photo);

            // Увеличиваем счетчик
            user.setPhotoCount(user.getPhotoCount() + 1);
            userRepository.save(user);

            log.info("Photo created for user {}: {}", userId, picId);

            return photoServiceMapper.toResponseDto(photo);

        } catch (IOException e) {
            log.error("Failed to upload image for user {}: {}", userId, e.getMessage(), e);
            throw new ImageUploadException("Ошибка загрузки изображения: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePhoto(Long userId, Long photoId) {
        Photo photo = photoRepository.findByIdAndAuthorId(photoId, userId)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found or access denied"));

        // Пытаемся удалить файл из хранилища
        boolean fileDeleted = storageService.deleteImage(photo.getPicId());
        if (!fileDeleted) {
            log.warn("Failed to delete image file with ID: {} - continuing with database cleanup", photo.getPicId());
        }

        // Всегда удаляем запись из БД
        photoRepository.delete(photo);
        userRepository.decrementPhotoCount(userId);

        log.info("Photo deleted from database: userId={}, photoId={}, fileDeleted={}",
                userId, photoId, fileDeleted);
    }

    @Override
    public PhotoResponseDto getPhotoById(Long userId, Long photoId) {
        // Находим фото с проверкой владельца
        Photo photo = photoRepository.findByIdAndAuthorId(photoId, userId)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found or access denied"));

        return photoServiceMapper.toResponseDto(photo);
    }

    @Override
    public List<PhotoResponseDto> getUserPhotos(Long userId) {
        List<Photo> photos = photoRepository.findByAuthorIdOrderByUploadDateDesc(userId);
        return photos.stream()
                .map(photoServiceMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    private String generateUniqueId() {
        return UUID.randomUUID().toString();
    }


}
