package com.example.cleopatra.dto.ForumComment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateForumCommentDto {

    @NotNull(message = "Forum ID обязателен")
    private Long forumId;

    @NotBlank(message = "Содержимое комментария не может быть пустым")
    @Size(min = 1, max = 5000, message = "Комментарий должен содержать от 1 до 5000 символов")
    private String content;

    // Опционально - для ответов на комментарии
    private Long parentId;
}