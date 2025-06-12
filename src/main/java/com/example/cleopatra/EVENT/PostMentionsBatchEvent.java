package com.example.cleopatra.EVENT;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PostMentionsBatchEvent {
    private final Long postId;
    private final Long mentionerUserId;  // кто упомянул
    private final String postContent;    // содержимое поста
    private final List<MentionInfo> mentions; // список упоминаний

    @Data
    @AllArgsConstructor
    public static class MentionInfo {
        private final Long mentionedUserId;
        private final String mentionText;
    }
}