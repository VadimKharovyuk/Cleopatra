package com.example.cleopatra.EVENT;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostLikedEvent {
    private final Long postId;
    private final Long postAuthorId;  // кому отправить уведомление
    private final Long likerUserId;   // кто лайкнул
    private final String postTitle;   // для текста уведомления
}
