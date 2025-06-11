package com.example.cleopatra.model;

import com.example.cleopatra.enums.ReportReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

///Жалобы на рекламу

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class AdReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Advertisement advertisement;

    @ManyToOne
    private User reporter; // кто пожаловался

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    private String description;
    private LocalDateTime reportedAt;
}


