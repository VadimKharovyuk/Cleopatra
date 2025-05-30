package com.example.cleopatra.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    /**
     * Загружает файл изображения и возвращает результат загрузки
     */
    StorageResult uploadImage(MultipartFile file) throws IOException;

    /**
     * Удаляет изображение по его идентификатору
     */
    boolean deleteImage(String imageId);

    /**
     * Получает информацию об изображении
     */
    Object getImageInfo(String imageId);

    /**
     * Результат загрузки изображения
     */
    class StorageResult {
        private String url;
        private String imageId;

        public StorageResult(String url, String imageId) {
            this.url = url;
            this.imageId = imageId;
        }

        public String getUrl() {
            return url;
        }

        public String getImageId() {
            return imageId;
        }
    }
}
