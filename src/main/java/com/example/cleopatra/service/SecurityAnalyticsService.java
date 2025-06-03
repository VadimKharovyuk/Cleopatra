package com.example.cleopatra.service;

import com.example.cleopatra.repository.VisitRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class SecurityAnalyticsService {
    private final VisitRepository visitRepository;
    private final GeoLocationService geoLocationService;

    /**
     * Проверить подозрительную активность
     */
    public boolean isIpSuspicious(String ipAddress, Long userId, LocalDateTime timeWindow) {
        // Проверка на множественные попытки с одного IP
        long visitCount = visitRepository.countByIpAddressAndVisitedAtAfter(
                ipAddress, timeWindow.minusHours(1));

        if (visitCount > 50) { // Более 50 визитов за час
            log.warn("Подозрительная активность с IP {}: {} визитов за час", ipAddress, visitCount);
            return true;
        }

        return false;
    }

    /**
     * Получить статистику по странам
     */
    public Map<String, Long> getVisitsByCountry(Long userId) {
        // Группировка визитов по странам (через геолокацию IP)
        return visitRepository.findAll().stream()
                .filter(visit -> visit.getVisitedUser().getId().equals(userId))
                .map(visit -> geoLocationService.getLocationByIp(visit.getIpAddress()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        GeoLocationService.GeoLocationDto::getCountry,
                        Collectors.counting()
                ));
    }

    @Data
    @Builder
    public static class ExtendedVisitStatsDto {
        // Базовая статистика
        private Long totalVisits;
        private Long todayVisits;
        private Long weekVisits;
        private Long monthVisits;
        private Long uniqueVisitorsCount;

        // НОВАЯ СТАТИСТИКА ПО IP
        private Long uniqueIpCount;
        private Map<String, Long> visitsByCountry;
        private Map<String, Long> visitsByDevice;
        private Map<String, Long> visitsByBrowser;
        private List<String> topIpAddresses;
        private Double averageVisitsPerIp;
    }
}

