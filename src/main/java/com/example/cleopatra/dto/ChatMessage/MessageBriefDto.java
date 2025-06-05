package com.example.cleopatra.dto.ChatMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageBriefDto {
    private Long id;
    private String content;
    private UserBriefDto sender;
    private LocalDateTime createdAt;

    /**
     * Автоматически генерирует короткий контент и включает в JSON
     */
    @JsonProperty("shortContent")
    public String getShortContent() {
        if (content == null) return "";
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }
}
