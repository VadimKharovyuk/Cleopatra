package com.example.cleopatra.dto.GroupDto;

import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGroupPostRequest {

    @Size(max = 5000, message = "Text cannot exceed 5000 characters")
    private String text;

    private MultipartFile imageUrl;

    @Deprecated
    private String imgId;
}