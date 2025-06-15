package com.example.cleopatra.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PhotoCreateDto {

    @NotNull(message = "Изображение обязательно")
    private MultipartFile image;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    private String description;
}