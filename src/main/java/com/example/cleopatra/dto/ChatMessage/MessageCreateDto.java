package com.example.cleopatra.dto.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageCreateDto {

    @NotNull(message = "ID получателя обязателен")
    private Long recipientId;

    @NotBlank(message = "Содержимое сообщения не может быть пустым")
    @Size(max = 5000, message = "Сообщение не может быть длиннее 5000 символов")
    private String content;

    // ID сообщения на которое отвечаем (опционально)
    private Long replyToMessageId;
}
