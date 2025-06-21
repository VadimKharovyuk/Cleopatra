package com.example.cleopatra.dto.Forum;

import com.example.cleopatra.enums.ForumType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ForumCreateResponseDTO {
    private Long id;
    private String title;
    private ForumType forumType;
    private LocalDateTime createdAt;
    private String message; // "Тема успешно создана"

}