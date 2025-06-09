package com.example.cleopatra.dto.Comment;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(min = 1, max = 500, message = "Комментарий должен содержать от 1 до 500 символов")
    private String content;


}
