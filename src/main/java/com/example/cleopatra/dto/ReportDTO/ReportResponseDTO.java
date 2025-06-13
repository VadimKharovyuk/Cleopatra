package com.example.cleopatra.dto.ReportDTO;

import com.example.cleopatra.enums.ReportReason;
import com.example.cleopatra.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportResponseDTO {
    private Long id;
    private Long postId;
    private String postContent; // первые 100 символов
    private String postAuthorName;
    private String reporterName;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private Integer priority;
    private String adminComment;
    private String actionTaken;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String reviewedByName;
}
