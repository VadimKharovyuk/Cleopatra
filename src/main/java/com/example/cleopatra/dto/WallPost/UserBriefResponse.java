package com.example.cleopatra.dto.WallPost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBriefResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String imageUrl;
}
