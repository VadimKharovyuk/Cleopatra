package com.example.cleopatra.dto.GroupDto;
import com.example.cleopatra.enums.GroupType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateGroupRequestDTO {

    @Size(min = 3, max = 100, message = "Название группы должно содержать от 3 до 100 символов")
    private String name;

    @Size(max = 500, message = "Описание не может превышать 500 символов")
    private String description;

    private GroupType groupType;
    private String imageUrl;
    private String imgId;
    private String backgroundImageUrl;
    private String backgroundImgId;
}
