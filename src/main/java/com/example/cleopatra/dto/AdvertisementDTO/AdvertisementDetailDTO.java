package com.example.cleopatra.dto.AdvertisementDTO;
import com.example.cleopatra.enums.AdCategory;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.model.Advertisement;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementDetailDTO {

    private Long id;
    private String title;
    private String description;
    private String shortDescription;
    private String url;
    private String imageUrl;

    // Финансовая информация (только для владельца)
    private BigDecimal totalBudget;
    private BigDecimal remainingBudget;
    private BigDecimal spentAmount;
    private BigDecimal costPerView;
    private BigDecimal costPerClick;

    // Статистика
    private Long viewsCount;
    private Long clicksCount;
    private Double ctr; // Click Through Rate
    private BigDecimal averageCostPerClick;

    // Таргетинг
    private Gender targetGender;
    private Integer minAge;
    private Integer maxAge;
    private String targetCity;
    private AdCategory category;

    // Время и статус
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private AdStatus status;

    private LocalDateTime lastViewedAt;
    private LocalDateTime lastClickedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Модерация
    private String approvedByName; // имя админа кто одобрил
    private LocalDateTime approvedAt;
    private Integer reportCount;

    public static AdvertisementDetailDTO fromEntity(Advertisement ad) {
        BigDecimal spentAmount = ad.getTotalBudget().subtract(ad.getRemainingBudget());
        Double ctr = ad.getViewsCount() > 0 ?
                (double) ad.getClicksCount() / ad.getViewsCount() * 100 : 0.0;
        BigDecimal avgCostPerClick = ad.getClicksCount() > 0 ?
                spentAmount.divide(new BigDecimal(ad.getClicksCount()), 2, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO;

        return AdvertisementDetailDTO.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .shortDescription(ad.getShortDescription())
                .url(ad.getUrl())
                .imageUrl(ad.getImageUrl())
                .totalBudget(ad.getTotalBudget())
                .remainingBudget(ad.getRemainingBudget())
                .spentAmount(spentAmount)
                .costPerView(ad.getCostPerView())
                .costPerClick(ad.getCostPerClick())
                .viewsCount(ad.getViewsCount())
                .clicksCount(ad.getClicksCount())
                .ctr(ctr)
                .averageCostPerClick(avgCostPerClick)
                .targetGender(ad.getTargetGender())
                .minAge(ad.getMinAge())
                .maxAge(ad.getMaxAge())
                .targetCity(ad.getTargetCity())
                .category(ad.getCategory())
                .startTime(ad.getStartTime())
                .endTime(ad.getEndTime())
                .startDate(ad.getStartDate())
                .endDate(ad.getEndDate())
                .status(ad.getStatus())
                .lastViewedAt(ad.getLastViewedAt())
                .lastClickedAt(ad.getLastClickedAt())
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .approvedByName(ad.getApprovedBy() != null ?
                        ad.getApprovedBy().getFirstName() + " " + ad.getApprovedBy().getLastName() : null)
                .reportCount(ad.getReports() != null ? ad.getReports().size() : 0)
                .build();
    }
}
