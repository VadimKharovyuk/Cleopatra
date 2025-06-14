package com.example.cleopatra.EVENT;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Событие создания новой записи на стене пользователя
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WallPostCreatedEvent {

    private Long postId;           // ID созданного поста
    private Long authorId;         // ID автора поста
    private String authorName;     // Имя автора поста
    private Long wallOwnerId;      // ID владельца стены
    private String postText;       // Текст поста (первые 100 символов для уведомления)
    private String postPicUrl;     // URL картинки поста (если есть)

}