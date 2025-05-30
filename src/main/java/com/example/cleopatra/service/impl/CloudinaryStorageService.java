package com.example.cleopatra.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.cleopatra.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryStorageService implements StorageService {
    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        try {
            String cloudinaryUrl = "cloudinary://" + apiKey + ":" + apiSecret + "@" + cloudName;
            cloudinary = new Cloudinary(cloudinaryUrl);
            log.info("Cloudinary инициализирован через URL-строку");
        } catch (Exception e) {
            log.error("Ошибка инициализации Cloudinary: {}", e.getMessage());
        }
    }

    @Override
    public StorageResult uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл отсутствует или пуст");
        }

        try {
            // Формируем уникальный идентификатор
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String originalFilename = file.getOriginalFilename();
            String fileBaseName = "image";

            if (originalFilename != null && !originalFilename.isEmpty()) {
                fileBaseName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
                int dotIndex = fileBaseName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileBaseName = fileBaseName.substring(0, dotIndex);
                }
            }

            Map<String, Object> params = ObjectUtils.asMap(
                    "resource_type", "auto",
                    "public_id", timestamp + "_" + fileBaseName,
                    "overwrite", true,
                    "tags", "user_upload"
            );

            log.info("Начинаем загрузку изображения в Cloudinary: {}", fileBaseName);

            // Загружаем изображение
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Изображение успешно загружено в Cloudinary. Public ID: {}", publicId);

            return new StorageResult(url, publicId);
        } catch (IOException e) {
            log.error("Ошибка ввода-вывода при загрузке изображения: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при загрузке изображения: {}", e.getMessage());
            throw new IOException("Ошибка при загрузке изображения в Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteImage(String imageId) {
        if (imageId == null || imageId.isEmpty()) {
            log.warn("Попытка удаления изображения с пустым publicId");
            return false;
        }

        try {
            log.info("Удаляем изображение из Cloudinary. Public ID: {}", imageId);

            Map result = cloudinary.uploader().destroy(imageId, ObjectUtils.emptyMap());
            boolean success = "ok".equals(result.get("result"));

            if (success) {
                log.info("Изображение успешно удалено из Cloudinary. Public ID: {}", imageId);
            } else {
                log.warn("Не удалось удалить изображение из Cloudinary. Public ID: {}, Результат: {}",
                        imageId, result);
            }

            return success;
        } catch (Exception e) {
            log.error("Ошибка при удалении изображения из Cloudinary. Public ID: {}, Ошибка: {}",
                    imageId, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getImageInfo(String imageId) {
        if (imageId == null || imageId.isEmpty()) {
            throw new IllegalArgumentException("Public ID не может быть пустым");
        }

        try {
            log.info("Получаем информацию об изображении. Public ID: {}", imageId);
            return cloudinary.api().resource(imageId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("Ошибка при получении информации об изображении. Public ID: {}, Ошибка: {}",
                    imageId, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
