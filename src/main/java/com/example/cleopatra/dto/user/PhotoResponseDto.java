package com.example.cleopatra.dto.user;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoResponseDto {
    private Long id;
    private String picUrl;
    private String picId;
    private LocalDateTime uploadDate;
    private String description;
    private Long authorId;
    private String authorName;
}
