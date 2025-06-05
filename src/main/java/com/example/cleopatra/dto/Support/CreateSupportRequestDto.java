package com.example.cleopatra.dto.Support;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSupportRequestDto {

    @NotBlank(message = "Заголовок не может быть пустым")
    @Size(max = 200, message = "Заголовок не может быть длиннее 200 символов")
    private String title;

    @NotBlank(message = "Описание не может быть пустым")
    @Size(max = 2000, message = "Описание не может быть длиннее 2000 символов")
    private String description;

    @NotNull(message = "ID пользователя обязателен")
    private Long userId;

    private String category = "OTHER";
}
