package com.example.cleopatra.dto.WallPost;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class WallPostCreateRequest {
    private String text;
    private Long wallOwnerId;
    private MultipartFile image;
}
