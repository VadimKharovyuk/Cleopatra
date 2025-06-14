package com.example.cleopatra.dto.WallPost;

import com.example.cleopatra.enums.WallAccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WallSettingsResponse {
    private Long userId;
    private WallAccessLevel wallAccessLevel;
    private String displayName;
}

