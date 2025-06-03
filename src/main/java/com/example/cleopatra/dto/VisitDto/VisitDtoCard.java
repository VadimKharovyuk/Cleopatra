package com.example.cleopatra.dto.VisitDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitDtoCard {
    private Long id;
    private Long visitorId;
    private String visitorFirstName;
    private String visitorLastName;
    private String visitorImageUrl;
    private String visitorCity;
    private Boolean visitorIsOnline;
    private LocalDateTime visitedAt;

}
