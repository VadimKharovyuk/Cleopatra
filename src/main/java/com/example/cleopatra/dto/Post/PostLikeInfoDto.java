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
public class PostLikeInfoDto {
    private Long postId;
    private Long likesCount;
    private Boolean isLikedByCurrentUser;
    private List<PostResponseDto.LikeUserDto> recentLikes;
}
