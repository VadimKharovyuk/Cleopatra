package com.example.cleopatra.dto.AdvertisementDTO;

import com.example.cleopatra.enums.AdCategory;
import com.example.cleopatra.model.Advertisement;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementResponseDTO {

    private Long id;
    private String title;
    private String shortDescription; // только краткое для пользователя
    private String url;
    private String imageUrl;
    private AdCategory category;

    // Публичная статистика (без финансов)
    private Long viewsCount;
    private Long clicksCount;

    // Метод для безопасного показа рекламы пользователю
    public static AdvertisementResponseDTO fromEntity(Advertisement ad) {
        return AdvertisementResponseDTO.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .shortDescription(ad.getShortDescription())
                .url(ad.getUrl())
                .imageUrl(ad.getImageUrl())
                .category(ad.getCategory())
                .viewsCount(ad.getViewsCount())
                .clicksCount(ad.getClicksCount())
                .build();
    }
}
