//package com.example.cleopatra.service;
//
//import com.example.cleopatra.ExistsException.ImageValidationException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.Set;
//
//@Component
//@Slf4j
//public class ImageValidator {
//
//    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
//    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
//            "image/jpeg",
//            "image/jpg",
//            "image/png"
//    );
//    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
//            ".jpg", ".jpeg", ".png"
//    );
//
//    /**
//     * Валидирует изображение по всем критериям
//     */
//    public void validateImage(MultipartFile file) {
//        log.debug("Начинаем валидацию изображения: {}",
//                file != null ? file.getOriginalFilename() : "null");
//
//        validateFileExists(file);
//        validateFileSize(file);
//        validateContentType(file);
//        validateFileExtension(file);
//        validateFileName(file);
//
//        log.debug("Валидация изображения успешно завершена: {}", file.getOriginalFilename());
//    }
//
//    /**
//     * Проверяет, что файл не null и не пустой
//     */
//    private void validateFileExists(MultipartFile file) {
//        if (file == null) {
//            throw new ImageValidationException("Файл изображения не предоставлен");
//        }
//
//        if (file.isEmpty()) {
//            throw new ImageValidationException("Файл изображения не может быть пустым");
//        }
//    }
//
//    /**
//     * Проверяет размер файла
//     */
//    private void validateFileSize(MultipartFile file) {
//        long fileSize = file.getSize();
//
//        if (fileSize > MAX_FILE_SIZE) {
//            String fileSizeMB = String.format("%.2f", fileSize / (1024.0 * 1024.0));
//            String maxSizeMB = String.format("%.0f", MAX_FILE_SIZE / (1024.0 * 1024.0));
//
//            throw new ImageValidationException(
//                    String.format("Размер файла (%s MB) превышает максимально допустимый (%s MB)",
//                            fileSizeMB, maxSizeMB)
//            );
//        }
//
//        if (fileSize == 0) {
//            throw new ImageValidationException("Файл изображения имеет нулевой размер");
//        }
//    }
//
//    /**
//     * Проверяет MIME-тип файла
//     */
//    private void validateContentType(MultipartFile file) {
//        String contentType = file.getContentType();
//
//        if (contentType == null || contentType.trim().isEmpty()) {
//            throw new ImageValidationException("Не удалось определить тип файла");
//        }
//
//        if (!contentType.startsWith("image/")) {
//            throw new ImageValidationException(
//                    String.format("Файл должен быть изображением. Получен тип: %s", contentType)
//            );
//        }
//
//        if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
//            throw new ImageValidationException(
//                    String.format("Неподдерживаемый формат изображения: %s. " +
//                            "Поддерживаются: %s", contentType, ALLOWED_CONTENT_TYPES)
//            );
//        }
//    }
//
//    /**
//     * Проверяет расширение файла
//     */
//    private void validateFileExtension(MultipartFile file) {
//        String originalFilename = file.getOriginalFilename();
//
//        if (originalFilename == null) {
//            return; // Уже проверено в validateFileName
//        }
//
//        String extension = getFileExtension(originalFilename);
//
//        if (extension.isEmpty()) {
//            throw new ImageValidationException("Файл должен иметь расширение");
//        }
//
//        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
//            throw new ImageValidationException(
//                    String.format("Неподдерживаемое расширение файла: %s. " +
//                            "Поддерживаются: %s", extension, ALLOWED_EXTENSIONS)
//            );
//        }
//    }
//
//    /**
//     * Проверяет имя файла
//     */
//    private void validateFileName(MultipartFile file) {
//        String originalFilename = file.getOriginalFilename();
//
//        if (originalFilename == null || originalFilename.trim().isEmpty()) {
//            throw new ImageValidationException("Имя файла не может быть пустым");
//        }
//
//        // Проверка на недопустимые символы
//        if (containsInvalidCharacters(originalFilename)) {
//            throw new ImageValidationException(
//                    "Имя файла содержит недопустимые символы. " +
//                            "Разрешены только буквы, цифры, точки, дефисы и подчеркивания"
//            );
//        }
//
//        // Проверка длины имени файла
//        if (originalFilename.length() > 255) {
//            throw new ImageValidationException("Имя файла слишком длинное (максимум 255 символов)");
//        }
//    }
//
//    /**
//     * Извлекает расширение файла
//     */
//    private String getFileExtension(String filename) {
//        if (filename == null || filename.isEmpty()) {
//            return "";
//        }
//
//        int lastDotIndex = filename.lastIndexOf('.');
//        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
//            return "";
//        }
//
//        return filename.substring(lastDotIndex);
//    }
//
//    /**
//     * Проверяет наличие недопустимых символов в имени файла
//     */
//    private boolean containsInvalidCharacters(String filename) {
//        // Разрешаем буквы, цифры, точки, дефисы, подчеркивания и пробелы
//        return !filename.matches("^[a-zA-Z0-9._\\s-]+$");
//    }
//
//    /**
//     * Проверяет, является ли файл изображением (без выбрасывания исключений)
//     */
//    public boolean isValidImage(MultipartFile file) {
//        try {
//            validateImage(file);
//            return true;
//        } catch (ImageValidationException e) {
//            log.debug("Файл не прошел валидацию: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * Возвращает максимально допустимый размер файла в байтах
//     */
//    public long getMaxFileSize() {
//        return MAX_FILE_SIZE;
//    }
//
//    /**
//     * Возвращает набор поддерживаемых MIME-типов
//     */
//    public Set<String> getAllowedContentTypes() {
//        return Set.copyOf(ALLOWED_CONTENT_TYPES);
//    }
//
//    /**
//     * Возвращает набор поддерживаемых расширений файлов
//     */
//    public Set<String> getAllowedExtensions() {
//        return Set.copyOf(ALLOWED_EXTENSIONS);
//    }
//}
package com.example.cleopatra.service;

import com.example.cleopatra.ExistsException.ImageValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
@Slf4j
public class ImageValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB (увеличено с 5MB)
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png"
    );

    /**
     * Валидирует изображение по всем критериям
     */
    public void validateImage(MultipartFile file) {
        log.debug("Начинаем валидацию изображения: {}",
                file != null ? file.getOriginalFilename() : "null");

        validateFileExists(file);
        validateFileSize(file);
        validateContentType(file);
        validateFileExtension(file);
        validateFileName(file);

        log.debug("Валидация изображения успешно завершена: {}", file.getOriginalFilename());
    }

    /**
     * Проверяет, что файл не null и не пустой
     */
    private void validateFileExists(MultipartFile file) {
        if (file == null) {
            throw new ImageValidationException("Файл изображения не предоставлен");
        }

        if (file.isEmpty()) {
            throw new ImageValidationException("Файл изображения не может быть пустым");
        }
    }

    /**
     * Проверяет размер файла
     */
    private void validateFileSize(MultipartFile file) {
        long fileSize = file.getSize();

        if (fileSize > MAX_FILE_SIZE) {
            String fileSizeMB = String.format("%.2f", fileSize / (1024.0 * 1024.0));
            String maxSizeMB = String.format("%.0f", MAX_FILE_SIZE / (1024.0 * 1024.0));

            throw new ImageValidationException(
                    String.format("Размер файла (%s MB) превышает максимально допустимый (%s MB)",
                            fileSizeMB, maxSizeMB)
            );
        }

        if (fileSize == 0) {
            throw new ImageValidationException("Файл изображения имеет нулевой размер");
        }
    }

    /**
     * Проверяет MIME-тип файла
     */
    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.trim().isEmpty()) {
            throw new ImageValidationException("Не удалось определить тип файла");
        }

        if (!contentType.startsWith("image/")) {
            throw new ImageValidationException(
                    String.format("Файл должен быть изображением. Получен тип: %s", contentType)
            );
        }

        if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ImageValidationException(
                    String.format("Неподдерживаемый формат изображения: %s. " +
                            "Поддерживаются: %s", contentType, ALLOWED_CONTENT_TYPES)
            );
        }
    }

    /**
     * Проверяет расширение файла
     */
    private void validateFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null) {
            return; // Уже проверено в validateFileName
        }

        String extension = getFileExtension(originalFilename);

        if (extension.isEmpty()) {
            throw new ImageValidationException("Файл должен иметь расширение");
        }

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ImageValidationException(
                    String.format("Неподдерживаемое расширение файла: %s. " +
                            "Поддерживаются: %s", extension, ALLOWED_EXTENSIONS)
            );
        }
    }

    /**
     * Проверяет имя файла
     */
    private void validateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new ImageValidationException("Имя файла не может быть пустым");
        }

        // Проверка на недопустимые символы
        if (containsInvalidCharacters(originalFilename)) {
            throw new ImageValidationException(
                    "Имя файла содержит недопустимые символы. " +
                            "Разрешены только буквы, цифры, точки, дефисы и подчеркивания"
            );
        }

        // Проверка длины имени файла
        if (originalFilename.length() > 255) {
            throw new ImageValidationException("Имя файла слишком длинное (максимум 255 символов)");
        }
    }

    /**
     * Извлекает расширение файла
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex);
    }

    /**
     * Проверяет наличие недопустимых символов в имени файла
     */
    private boolean containsInvalidCharacters(String filename) {
        // Разрешаем буквы, цифры, точки, дефисы, подчеркивания и пробелы
        return !filename.matches("^[a-zA-Z0-9._\\s-]+$");
    }

    /**
     * Проверяет, является ли файл изображением (без выбрасывания исключений)
     */
    public boolean isValidImage(MultipartFile file) {
        try {
            validateImage(file);
            return true;
        } catch (ImageValidationException e) {
            log.debug("Файл не прошел валидацию: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Возвращает максимально допустимый размер файла в байтах
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * Возвращает максимально допустимый размер файла в мегабайтах (для удобства)
     */
    public int getMaxFileSizeMB() {
        return (int) (MAX_FILE_SIZE / (1024 * 1024));
    }

    /**
     * Возвращает набор поддерживаемых MIME-типов
     */
    public Set<String> getAllowedContentTypes() {
        return Set.copyOf(ALLOWED_CONTENT_TYPES);
    }

    /**
     * Возвращает набор поддерживаемых расширений файлов
     */
    public Set<String> getAllowedExtensions() {
        return Set.copyOf(ALLOWED_EXTENSIONS);
    }

    /**
     * Возвращает человекочитаемое описание ограничений для отображения в UI
     */
    public String getValidationRulesDescription() {
        return String.format(
                "Максимальный размер: %d MB. Поддерживаемые форматы: %s",
                getMaxFileSizeMB(),
                String.join(", ", ALLOWED_EXTENSIONS)
        );
    }

    /**
     * Логирует информацию о файле для отладки
     */
    public void logFileInfo(MultipartFile file) {
        if (file != null) {
            log.info("Информация о файле: имя='{}', размер={} байт ({:.2f} MB), тип='{}'",
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getSize() / (1024.0 * 1024.0),
                    file.getContentType()
            );
        }
    }
}