package com.example.cleopatra.model;

import com.example.cleopatra.enums.AdCategory;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "advertisements")
public class Advertisement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String shortDescription;
    private String url;
    private String imageUrl;
    private String imgId;


    @Builder.Default
    private BigDecimal totalBudget = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal remainingBudget = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal costPerView = new BigDecimal("0.20");
    @Builder.Default
    private BigDecimal costPerClick = new BigDecimal("0.50");




    @Builder.Default
    private Long viewsCount = 0L;
    @Builder.Default
    private Long clicksCount = 0L;
    private LocalDateTime lastViewedAt;
    private LocalDateTime lastClickedAt;

    // Детальная статистика (для глубокой аналитики)
    @OneToMany(mappedBy = "advertisement", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<AdStatistics> detailedStats = new ArrayList<>();



    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "admin_notes")
    private String adminNotes;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;


    // Таргетинг
    @Enumerated(EnumType.STRING)
    private Gender targetGender;
    private Integer minAge;
    private Integer maxAge;
    private String targetCity; // если null - для всех городов

    @Enumerated(EnumType.STRING)
    private AdCategory category;

    // Время показа
    private LocalTime startTime; // 19:00
    private LocalTime endTime;   // 21:00
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AdStatus status = AdStatus.PENDING;

    // Связи
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // Система жалоб
    @OneToMany(mappedBy = "advertisement", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AdReport> reports = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;




//    @Scheduled(fixedRate = 300000) // каждые 5 минут
//    public void checkBudgetStatus() {
//        List<Advertisement> activeAds = advertisementRepository.findByStatus(AdStatus.ACTIVE);
//
//        activeAds.stream()
//                .filter(Advertisement::isBudgetExhausted)
//                .forEach(ad -> {
//                    ad.setStatus(AdStatus.FINISHED);
//                    advertisementRepository.save(ad);
//                });
//    }
}
