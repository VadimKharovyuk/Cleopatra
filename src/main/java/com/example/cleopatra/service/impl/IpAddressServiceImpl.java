package com.example.cleopatra.service.impl;


import com.example.cleopatra.service.IpAddressService;
import com.example.cleopatra.service.NotificationService;
import com.example.cleopatra.service.VisitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class IpAddressServiceImpl implements IpAddressService {

    private final VisitService visitService;
    private final NotificationService notificationService;

    @Override
    public String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            log.warn("HttpServletRequest is null");
            return "unknown";
        }

        // Проверяем заголовки прокси
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (isValidIp(xForwardedFor)) {
            // Берем первый IP из списка (может быть несколько через запятую)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (isValidIp(xRealIp)) {
            return xRealIp.trim();
        }

        String xForwarded = request.getHeader("X-Forwarded");
        if (isValidIp(xForwarded)) {
            return xForwarded.trim();
        }

        String forwarded = request.getHeader("Forwarded");
        if (isValidIp(forwarded)) {
            // Forwarded заголовок может содержать "for=192.168.1.1"
            if (forwarded.contains("for=")) {
                String[] parts = forwarded.split("for=");
                if (parts.length > 1) {
                    String ip = parts[1].split(";")[0].trim();
                    // Убираем кавычки если есть
                    ip = ip.replaceAll("\"", "");
                    return ip;
                }
            }
            return forwarded.trim();
        }

        // Если ничего не найдено, возвращаем стандартный IP
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    @Override
    public String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && !userAgent.trim().isEmpty() ? userAgent : "Unknown";
    }

    @Override

    public void recordUserVisit(Long visitedUserId, Long currentUserId, HttpServletRequest request) {
        log.info("🔍 START recordUserVisit: visitedUserId={}, currentUserId={}", visitedUserId, currentUserId);

        if (visitedUserId == null || currentUserId == null || request == null) {
            log.warn("❌ Invalid parameters for recording visit");
            return;
        }

        // Проверка что не посещаем сами себя
        if (Objects.equals(visitedUserId, currentUserId)) {
            log.debug("🚫 User visiting own profile, skipping");
            return;
        }

        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = getUserAgent(request);

            log.info("📝 Recording visit: {} -> {} from IP: {}", currentUserId, visitedUserId, ipAddress);
            visitService.recordVisit(visitedUserId, currentUserId, ipAddress, userAgent);

            log.info("🔔 Creating notification: visitor={}, visited={}", currentUserId, visitedUserId);
            notificationService.createProfileVisitNotification(visitedUserId, currentUserId);

            log.info("✅ Visit and notification processed successfully");

        } catch (Exception e) {
            log.error("❌ Error in recordUserVisit", e);
        }
    }

    /**
     * Проверка валидности IP адреса
     */
    private boolean isValidIp(String ip) {
        return ip != null &&
                !ip.trim().isEmpty() &&
                !"unknown".equalsIgnoreCase(ip) &&
                !"null".equalsIgnoreCase(ip);
    }
}
