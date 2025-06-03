package com.example.cleopatra.service;

import com.example.cleopatra.ExistsException.ImageConversionException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
@Slf4j
public class ImageConverterService {

    private static final Set<String> CONVERTIBLE_TYPES = Set.of(
            "image/heif",
            "image/heic"
    );

    private static final String TARGET_FORMAT = "jpeg";
    private static final String TARGET_CONTENT_TYPE = "image/jpeg";
    private static final String TARGET_EXTENSION = ".jpg";

    public boolean needsConversion(String contentType) {
        return CONVERTIBLE_TYPES.contains(contentType.toLowerCase());
    }

    public ProcessedImage convertToStandardFormat(MultipartFile file) {
        try {
            log.info("Converting file {} from {} to JPEG",
                    file.getOriginalFilename(), file.getContentType());

            // Читаем исходное изображение
            BufferedImage sourceImage = readHeifImage(file);

            // Конвертируем в JPEG
            byte[] convertedBytes = convertToJpeg(sourceImage);

            // Генерируем новое имя файла
            String newFileName = generateNewFileName(file.getOriginalFilename());

            log.info("Successfully converted {} to JPEG, size: {} bytes",
                    file.getOriginalFilename(), convertedBytes.length);

            return ProcessedImage.builder()
                    .bytes(convertedBytes)
                    .contentType(TARGET_CONTENT_TYPE)
                    .fileName(newFileName)
                    .extension(TARGET_EXTENSION)
                    .originalFileName(file.getOriginalFilename())
                    .originalContentType(file.getContentType())
                    .build();

        } catch (Exception e) {
            log.error("Failed to convert image {}: {}",
                    file.getOriginalFilename(), e.getMessage(), e);
            throw new ImageConversionException(
                    "Failed to convert HEIF image to JPEG: " + e.getMessage(), e);
        }
    }

    private BufferedImage readHeifImage(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new ImageConversionException(
                        "Unable to read HEIF image. ImageIO returned null");
            }

            return image;
        }
    }

    private byte[] convertToJpeg(BufferedImage sourceImage) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Создаем новое изображение с RGB цветовым пространством
            BufferedImage rgbImage = new BufferedImage(
                    sourceImage.getWidth(),
                    sourceImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            // Копируем изображение, убирая прозрачность если есть
            Graphics2D g2d = rgbImage.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            g2d.drawImage(sourceImage, 0, 0, null);
            g2d.dispose();

            // Настраиваем качество JPEG
            ImageWriter writer = ImageIO.getImageWritersByFormatName(TARGET_FORMAT).next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(0.9f);

            // Записываем в поток
            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(rgbImage, null, null), writeParam);

            writer.dispose();
            imageOutputStream.close();

            return outputStream.toByteArray();
        }
    }

    private String generateNewFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            return "converted_image" + TARGET_EXTENSION;
        }

        String nameWithoutExtension = originalFileName.replaceAll("\\.[^.]*$", "");
        return nameWithoutExtension + TARGET_EXTENSION;
    }

    // Статический внутренний класс
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessedImage {
        private byte[] bytes;
        private String contentType;
        private String fileName;
        private String extension;
        private String originalFileName;
        private String originalContentType;

        public static ProcessedImage fromMultipartFile(MultipartFile file) {
            try {
                return ProcessedImage.builder()
                        .bytes(file.getBytes())
                        .contentType(file.getContentType())
                        .fileName(file.getOriginalFilename())
                        .extension(getExtensionFromFileName(file.getOriginalFilename()))
                        .originalFileName(file.getOriginalFilename())
                        .originalContentType(file.getContentType())
                        .build();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file bytes", e);
            }
        }

        private static String getExtensionFromFileName(String fileName) {
            if (fileName == null || !fileName.contains(".")) {
                return "";
            }
            return fileName.substring(fileName.lastIndexOf("."));
        }
    }
}