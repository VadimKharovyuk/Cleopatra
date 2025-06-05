package com.example.cleopatra.dto.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeResponseDto {
    private Long postId;
    private Boolean isLiked;
    private Long likesCount;
}

