package com.example.cleopatra.util;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class DeviceFingerprintUtil {

    /**
     * Генерирует отпечаток устройства на основе заголовков HTTP
     */
    public static String generate(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();

        // User-Agent
        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.hasText(userAgent)) {
            fingerprint.append(userAgent);
        }

        // Accept-Language
        String acceptLanguage = request.getHeader("Accept-Language");
        if (StringUtils.hasText(acceptLanguage)) {
            fingerprint.append("|").append(acceptLanguage);
        }

        // Accept-Encoding
        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (StringUtils.hasText(acceptEncoding)) {
            fingerprint.append("|").append(acceptEncoding);
        }

        return fingerprint.toString();
    }

    /**
     * Определяет название устройства по User-Agent
     */
    public static String getDeviceName(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null) {
            return "Неизвестное устройство";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("iphone")) {
            return "iPhone";
        } else if (userAgent.contains("ipad")) {
            return "iPad";
        } else if (userAgent.contains("android")) {
            return "Android устройство";
        } else if (userAgent.contains("chrome")) {
            return "Chrome браузер";
        } else if (userAgent.contains("firefox")) {
            return "Firefox браузер";
        } else if (userAgent.contains("safari")) {
            return "Safari браузер";
        }

        return "Веб браузер";
    }
}
