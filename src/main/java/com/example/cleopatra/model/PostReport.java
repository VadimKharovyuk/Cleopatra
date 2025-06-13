package com.example.cleopatra.model;
import com.example.cleopatra.enums.ReportReason;
import com.example.cleopatra.enums.ReportStatus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_reports")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Пост, на который жалуются
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // Кто пожаловался
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // Причина жалобы
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReportReason reason;

    // Дополнительное описание от пользователя
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Статус рассмотрения
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    // Кто рассматривает/рассмотрел жалобу (админ/модератор)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    // Комментарий от админа
    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    // Действие, предпринятое по жалобе
    @Column(name = "action_taken")
    private String actionTaken;

    // Приоритет (1-5, где 5 - самый высокий)
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 1;

    // IP адрес жалующегося (для анализа спама)
    @Column(name = "reporter_ip")
    private String reporterIp;

    // Системные поля
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status != ReportStatus.PENDING && reviewedAt == null) {
            reviewedAt = LocalDateTime.now();
        }
    }

    // Вспомогательные методы
    public boolean isPending() {
        return status == ReportStatus.PENDING;
    }

    public boolean isResolved() {
        return status == ReportStatus.RESOLVED;
    }

    public boolean isHighPriority() {
        return priority >= 4;
    }
}
