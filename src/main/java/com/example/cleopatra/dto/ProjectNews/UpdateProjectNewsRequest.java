package com.example.cleopatra.dto.ProjectNews;


import com.example.cleopatra.enums.NewsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateProjectNewsRequest {

    @NotBlank(message = "Заголовок не может быть пустым")
    @Size(max = 500, message = "Заголовок не может превышать 500 символов")
    private String title;

    @NotBlank(message = "Описание не может быть пустым")
    private String description;

    @Size(max = 255, message = "Краткое описание не может превышать 255 символов")
    private String shortDescription;

    @NotNull(message = "Тип новости обязателен")
    private NewsType newsType;

    private MultipartFile photo;
    private Boolean isPublished;

    private Boolean removePhoto = false;
}
