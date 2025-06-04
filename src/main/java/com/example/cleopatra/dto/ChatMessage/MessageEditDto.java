package com.example.cleopatra.dto.ChatMessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageEditDto {

    @NotBlank(message = "Содержимое сообщения не может быть пустым")
    @Size(max = 5000, message = "Сообщение не может быть длиннее 5000 символов")
    private String content;
}
