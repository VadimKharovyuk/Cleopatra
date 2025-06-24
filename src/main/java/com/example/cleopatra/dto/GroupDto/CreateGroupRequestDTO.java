package com.example.cleopatra.dto.GroupDto;

import com.example.cleopatra.enums.GroupType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupRequestDTO {

    @NotBlank(message = "Название группы не может быть пустым")
    @Size(min = 3, max = 100, message = "Название группы должно содержать от 3 до 100 символов")
    private String name;

    @Size(max = 500, message = "Описание не может превышать 500 символов")
    private String description;

    @NotNull(message = "Тип группы обязателен")
    private GroupType groupType;



    private MultipartFile image;
    private String imgId;


    private MultipartFile backgroundImage;
    private String backgroundImgId;
}