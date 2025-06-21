package com.example.cleopatra.dto.Forum;

import com.example.cleopatra.enums.ForumType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumCreateDTO {
    @NotBlank(message = "Заголовок обязателен")
    @Size(max = 255, message = "Заголовок не должен превышать 255 символов")
    private String title;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    @NotNull(message = "Тип форума обязателен")
    private ForumType forumType;

}
