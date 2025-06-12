package com.example.cleopatra.dto.UserMention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MentionDto {
    private Long id;
    private Long postId;
    private UserMentionDto mentionedUser;
    private UserMentionDto mentionedBy;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserMentionDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String imageUrl;
        private String email;

        public String getFullName() {
            return firstName + " " + lastName;
        }

        public String getInitials() {
            String first = firstName != null && !firstName.isEmpty() ? firstName.substring(0, 1) : "";
            String last = lastName != null && !lastName.isEmpty() ? lastName.substring(0, 1) : "";
            return (first + last).toUpperCase();
        }
    }
}