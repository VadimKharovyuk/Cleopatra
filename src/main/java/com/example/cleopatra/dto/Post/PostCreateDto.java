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

    @NotBlank(message = "Content cannot be empty")
    @Size()
    private String content;

    private MultipartFile  image;
}
