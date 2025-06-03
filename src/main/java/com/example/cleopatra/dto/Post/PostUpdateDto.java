package com.example.cleopatra.dto.Post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class PostUpdateDto {

    @NotNull(message = "Post ID is required")
    private Long id;

    @Size(max = 1000, message = "Content cannot exceed 1000 characters")
    private String content;

    private MultipartFile  image;
}
