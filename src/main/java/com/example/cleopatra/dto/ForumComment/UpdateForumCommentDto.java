package com.example.cleopatra.dto.ForumComment;

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
public class UpdateForumCommentDto {

    @NotBlank(message = "Содержимое комментария не может быть пустым")
    @Size(min = 1, max = 5000, message = "Комментарий должен содержать от 1 до 5000 символов")
    private String content;
}
