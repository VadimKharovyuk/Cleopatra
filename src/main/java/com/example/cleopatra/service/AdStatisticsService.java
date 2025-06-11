package com.example.cleopatra.service;

import com.example.cleopatra.enums.DeviceType;
import com.example.cleopatra.enums.StatType;
import com.example.cleopatra.model.AdStatistics;
import com.example.cleopatra.model.Advertisement;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.AdStatisticsRepository;
import com.example.cleopatra.repository.AdvertisementRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
@RequiredArgsConstructor
@Service
@Transactional
public class AdStatisticsService {

    private final AdStatisticsRepository adStatisticsRepository;
    private final AdvertisementRepository advertisementRepository;

    public void recordView(Long adId, User user, HttpServletRequest request) {
        Advertisement ad = advertisementRepository.findById(adId).orElseThrow();

        // Обновляем простые счетчики
        ad.setViewsCount(ad.getViewsCount() + 1);
        ad.setLastViewedAt(LocalDateTime.now());

        // Записываем детальную статистику
        AdStatistics stat = AdStatistics.builder()
                .advertisement(ad)
                .user(user)
                .type(StatType.VIEW)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .referer(request.getHeader("Referer"))
                .deviceType(detectDeviceType(request))
                .cost(ad.getCostPerView())
                .build();

        adStatisticsRepository.save(stat);
        advertisementRepository.save(ad);
    }

    public void recordClick(Long adId, User user, HttpServletRequest request) {
        Advertisement ad = advertisementRepository.findById(adId).orElseThrow();

        // Обновляем простые счетчики
        ad.setClicksCount(ad.getClicksCount() + 1);
        ad.setLastClickedAt(LocalDateTime.now());

        // Записываем детальную статистику
        AdStatistics stat = AdStatistics.builder()
                .advertisement(ad)
                .user(user)
                .type(StatType.CLICK)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .referer(request.getHeader("Referer"))
                .deviceType(detectDeviceType(request))
                .cost(ad.getCostPerClick())
                .build();

        adStatisticsRepository.save(stat);
        advertisementRepository.save(ad);
    }

    private DeviceType detectDeviceType(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return DeviceType.UNKNOWN;

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile")) return DeviceType.MOBILE;
        if (userAgent.contains("tablet")) return DeviceType.TABLET;
        return DeviceType.DESKTOP;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
