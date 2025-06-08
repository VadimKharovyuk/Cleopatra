package com.example.cleopatra.EVENT;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostCommentEvent {
    private final Long postId;           // ID поста, к которому добавлен комментарий
    private final Long postAuthorId;     // ID автора поста (кому отправить уведомление)
    private final Long commenterUserId;  // ID пользователя, который оставил комментарий
    private final Long commentId;        // ID самого комментария
    private final String commentText;    // Текст комментария (для превью в уведомлении)
}
