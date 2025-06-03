package com.example.cleopatra.dto.VisitDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitStatsDto {
    private Long totalVisits;
    private Long todayVisits;
    private Long weekVisits;
    private Long monthVisits;
    private Long uniqueVisitorsCount;
}
