package com.example.cleopatra.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface DatabaseStorageService {
    // Специфично для БД
    byte[] getImageData(String imageId);
    String getContentType(String imageId);

    // Для историй
    StorageService.StorageResult saveStoryImage(MultipartFile file) throws IOException;
    boolean deleteStoryImage(String imageId);

    // Batch операции для шедулера
    List<String> getExpiredStoryImages();
    int deleteExpiredStories();
}
