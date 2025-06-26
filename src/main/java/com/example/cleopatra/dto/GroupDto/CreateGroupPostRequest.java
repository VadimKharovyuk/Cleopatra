package com.example.cleopatra.dto.GroupDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupPostRequest {

    @NotNull(message = "Group ID is required")
    private Long groupId;

    @Size(max = 5000, message = "Text cannot exceed 5000 characters")
    private String text;

    private MultipartFile imageUrl;

    private String imgId;
}
