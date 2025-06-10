package com.example.cleopatra.dto.Post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostCreateDto {

    @NotBlank(message = "Содержимое поста не может быть пустым")
    @Size(max = 5000, message = "Содержимое поста не может превышать 5000 символов")
    private String content;

    private MultipartFile image;

    // Поля для геолокации
    private Long locationId;
    private Double latitude;
    private Double longitude;
    private String placeName;

    // ✅ ДОБАВЛЯЕМ метод для логирования
    @Override
    public String toString() {
        return String.format("PostCreateDto{content='%s', locationId=%s, latitude=%s, longitude=%s, placeName='%s'}",
                content != null ? content.substring(0, Math.min(50, content.length())) + "..." : null,
                locationId, latitude, longitude, placeName);
    }
}
