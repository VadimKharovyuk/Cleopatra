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

    // НОВОЕ ПОЛЕ
    private String userAgent;

    // МЕТОДЫ ДЛЯ ОПРЕДЕЛЕНИЯ УСТРОЙСТВА И БРАУЗЕРА

    /**
     * Получить тип устройства на основе User-Agent
     */
    public String getDeviceType() {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        // Мобильные устройства
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") ||
                ua.contains("ipod") || ua.contains("blackberry") || ua.contains("windows phone")) {
            return "Mobile";
        }

        // Планшеты
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        }

        // Десктоп
        return "Desktop";
    }

    /**
     * Получить иконку устройства
     */
    public String getDeviceIcon() {
        String deviceType = getDeviceType();
        switch (deviceType) {
            case "Mobile":
                return "fas fa-mobile-alt";
            case "Tablet":
                return "fas fa-tablet-alt";
            case "Desktop":
                return "fas fa-desktop";
            default:
                return "fas fa-question-circle";
        }
    }

    /**
     * Получить браузер на основе User-Agent
     */
    public String getBrowser() {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        // Порядок проверки важен! Сначала более специфичные
        if (ua.contains("edg/")) {
            return "Edge";
        } else if (ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        } else if (ua.contains("chrome/") && !ua.contains("edg")) {
            return "Chrome";
        } else if (ua.contains("firefox/")) {
            return "Firefox";
        } else if (ua.contains("safari/") && !ua.contains("chrome")) {
            return "Safari";
        } else if (ua.contains("trident") || ua.contains("msie")) {
            return "Internet Explorer";
        }

        return "Other";
    }

    /**
     * Получить иконку браузера
     */
    public String getBrowserIcon() {
        String browser = getBrowser();
        switch (browser) {
            case "Chrome":
                return "fab fa-chrome";
            case "Firefox":
                return "fab fa-firefox-browser";
            case "Safari":
                return "fab fa-safari";
            case "Edge":
                return "fab fa-edge";
            case "Opera":
                return "fab fa-opera";
            case "Internet Explorer":
                return "fab fa-internet-explorer";
            default:
                return "fas fa-globe";
        }
    }

    /**
     * Получить операционную систему
     */
    public String getOperatingSystem() {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("windows nt 10")) {
            return "Windows 10/11";
        } else if (ua.contains("windows nt 6.3")) {
            return "Windows 8.1";
        } else if (ua.contains("windows nt 6.2")) {
            return "Windows 8";
        } else if (ua.contains("windows nt 6.1")) {
            return "Windows 7";
        } else if (ua.contains("windows")) {
            return "Windows";
        } else if (ua.contains("mac os x")) {
            return "macOS";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            return "iOS";
        } else if (ua.contains("linux")) {
            return "Linux";
        }

        return "Other";
    }

    /**
     * Получить иконку операционной системы
     */
    public String getOsIcon() {
        String os = getOperatingSystem();
        if (os.startsWith("Windows")) {
            return "fab fa-windows";
        } else if (os.equals("macOS")) {
            return "fab fa-apple";
        } else if (os.equals("Android")) {
            return "fab fa-android";
        } else if (os.equals("iOS")) {
            return "fab fa-apple";
        } else if (os.equals("Linux")) {
            return "fab fa-linux";
        }
        return "fas fa-question-circle";
    }

    /**
     * Получить краткое описание устройства для отображения
     */
    public String getDeviceInfo() {
        return getDeviceType() + " • " + getBrowser() + " • " + getOperatingSystem();
    }

    /**
     * Получить полное имя посетителя
     */
    public String getVisitorFullName() {
        StringBuilder name = new StringBuilder();
        if (visitorFirstName != null && !visitorFirstName.trim().isEmpty()) {
            name.append(visitorFirstName);
        }
        if (visitorLastName != null && !visitorLastName.trim().isEmpty()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(visitorLastName);
        }
        return name.length() > 0 ? name.toString() : "Пользователь";
    }
}
