package com.example.cleopatra.dto.AdvertisementDTO;

import com.example.cleopatra.enums.AdCategory;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.model.Advertisement;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementListDTO {

    private Long id;
    private String title;
    private String shortDescription;
    private AdCategory category;
    private AdStatus status;

    // Основная статистика
    private BigDecimal totalBudget;
    private BigDecimal remainingBudget;
    private Long viewsCount;
    private Long clicksCount;
    private Double ctr;

    // Создатель
    private String createdByName;
    private String createdByEmail;

    // Даты
    private LocalDateTime createdAt;
    private LocalDateTime lastViewedAt;

    // Для админов
    private Integer reportCount;
    private Boolean needsReview;

    public static AdvertisementListDTO fromEntity(Advertisement ad) {
        // Безопасный расчет CTR
        Double ctr = 0.0;
        if (ad.getViewsCount() != null && ad.getViewsCount() > 0) {
            ctr = (double) ad.getClicksCount() / ad.getViewsCount() * 100;
            // Округление до 2 знаков
            ctr = Math.round(ctr * 100.0) / 100.0;
        }

        // Безопасное получение имени создателя
        String createdByName = "Неизвестно";
        String createdByEmail = "Неизвестно";
        if (ad.getCreatedBy() != null) {
            createdByName = (ad.getCreatedBy().getFirstName() != null ? ad.getCreatedBy().getFirstName() : "")
                    + " " +
                    (ad.getCreatedBy().getLastName() != null ? ad.getCreatedBy().getLastName() : "");
            createdByName = createdByName.trim();
            if (createdByName.isEmpty()) {
                createdByName = "Пользователь #" + ad.getCreatedBy().getId();
            }
            createdByEmail = ad.getCreatedBy().getEmail() != null ? ad.getCreatedBy().getEmail() : "Неизвестно";
        }

        return AdvertisementListDTO.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .shortDescription(ad.getShortDescription())
                .category(ad.getCategory())
                .status(ad.getStatus())
                .totalBudget(ad.getTotalBudget())
                .remainingBudget(ad.getRemainingBudget())
                .viewsCount(ad.getViewsCount() != null ? ad.getViewsCount() : 0L)
                .clicksCount(ad.getClicksCount() != null ? ad.getClicksCount() : 0L)
                .ctr(ctr)
                .createdByName(createdByName)
                .createdByEmail(createdByEmail)
                .createdAt(ad.getCreatedAt())
                .lastViewedAt(ad.getLastViewedAt())
                .reportCount(ad.getReports() != null ? ad.getReports().size() : 0)
                .needsReview(ad.getReports() != null && !ad.getReports().isEmpty())
                .build();
    }
}