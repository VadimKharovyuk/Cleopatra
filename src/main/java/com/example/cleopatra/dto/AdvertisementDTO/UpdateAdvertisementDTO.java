package com.example.cleopatra.dto.AdvertisementDTO;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdvertisementDTO {

    @Size(min = 5, max = 100)
    private String title;

    @Size(min = 10, max = 500)
    private String description;

    @Size(min = 5, max = 150)
    private String shortDescription;

    @Pattern(regexp = "^https?://.*")
    private String url;

    // Можно изменять бюджет (только увеличивать)
    @DecimalMin(value = "0.01")
    private BigDecimal additionalBudget;

    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate endDate; // можно продлить кампанию
}
