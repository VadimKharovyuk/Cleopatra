package com.example.cleopatra.dto.WallPost;

import com.example.cleopatra.enums.WallAccessLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WallSettingsUpdateRequest {

    @NotNull(message = "Уровень доступа обязателен")
    private WallAccessLevel wallAccessLevel;

    // Будущие настройки стены
    // private Boolean allowComments;
    // private Boolean allowPhotos;
    // private Boolean moderateComments;
}
