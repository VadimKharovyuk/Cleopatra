package com.example.cleopatra.controller;

import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.TrustedDeviceService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/devices")
@RequiredArgsConstructor
@Slf4j
public class TrustedDeviceController {

    private final TrustedDeviceService trustedDeviceService;
    private final UserRepository userRepository;


    @GetMapping("/test")
    public String testPage() {
        return "devices/test";
    }

    /**
     * GET /devices - Страница управления доверенными устройствами
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String devicesPage(Model model, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return "redirect:/login?error=user_not_found";
            }

            List<TrustedDeviceService.TrustedDeviceDto> devices =
                    trustedDeviceService.getTrustedDevices(userId);

            model.addAttribute("devices", devices);
            model.addAttribute("devicesCount", devices.size());

            return "devices/manage";

        } catch (Exception e) {
            log.error("Error loading devices page: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка при загрузке устройств");
            return "devices/manage";
        }
    }


    /**
     * GET /devices/api/list - API: Получить список доверенных устройств
     */
    @GetMapping("/api/list")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTrustedDevices(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Пользователь не найден"));
            }

            List<TrustedDeviceService.TrustedDeviceDto> devices =
                    trustedDeviceService.getTrustedDevices(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "devices", devices,
                    "count", devices.size()
            ));

        } catch (Exception e) {
            log.error("Error getting trusted devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Ошибка при получении устройств"));
        }
    }

    /**
     * GET /devices/api/current - API: Информация о текущем устройстве
     */
    @GetMapping("/api/current")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentDeviceInfo(HttpServletRequest request, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Пользователь не найден"));
            }

            String deviceId = getDeviceIdFromCookie(request);

            if (deviceId == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "trusted", false,
                        "message", "Устройство не имеет cookie",
                        "deviceInfo", Map.of(
                                "userAgent", request.getHeader("User-Agent"),
                                "deviceName", extractDeviceName(request.getHeader("User-Agent"))
                        )
                ));
            }

            Optional<com.example.cleopatra.model.TrustedDevice> deviceOpt =
                    trustedDeviceService.getTrustedDevice(deviceId, userId);

            if (deviceOpt.isPresent()) {
                com.example.cleopatra.model.TrustedDevice device = deviceOpt.get();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "trusted", true,
                        "deviceId", deviceId.substring(0, 8) + "...",
                        "deviceName", device.getDeviceName(),
                        "lastUsed", device.getLastUsedAt(),
                        "created", device.getCreatedAt()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "trusted", false,
                        "deviceId", deviceId.substring(0, 8) + "...",
                        "deviceInfo", Map.of(
                                "userAgent", request.getHeader("User-Agent"),
                                "deviceName", extractDeviceName(request.getHeader("User-Agent"))
                        )
                ));
            }

        } catch (Exception e) {
            log.error("Error getting current device info: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Ошибка при получении информации об устройстве"));
        }
    }

    /**
     * POST /devices/api/trust - API: Добавить текущее устройство в доверенные
     */
    @PostMapping("/api/trust")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> trustCurrentDevice(@RequestBody TrustDeviceRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse,
                                                Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Пользователь не найден"));
            }

            String deviceId = getDeviceIdFromCookie(httpRequest);
            if (deviceId == null) {
                deviceId = UUID.randomUUID().toString();
                setDeviceIdCookie(httpResponse, deviceId);
            }

            // Используем переданное имя или определяем автоматически
            String deviceName = request.getDeviceName();
            if (deviceName == null || deviceName.trim().isEmpty()) {
                deviceName = extractDeviceName(httpRequest.getHeader("User-Agent"));
            }

            boolean added = trustedDeviceService.addTrustedDevice(
                    userId, deviceId, deviceName, httpRequest);

            if (added) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Устройство добавлено в доверенные",
                        "deviceId", deviceId.substring(0, 8) + "...",
                        "deviceName", deviceName
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Не удалось добавить устройство в доверенные"));
            }

        } catch (Exception e) {
            log.error("Error trusting current device: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Ошибка при добавлении устройства"));
        }
    }

    /**
     * DELETE /devices/api/{deviceId} - API: Удаление доверенного устройства
     */
    @DeleteMapping("/api/{deviceId}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> revokeTrustedDevice(@PathVariable Long deviceId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Пользователь не найден"));
            }

            boolean revoked = trustedDeviceService.revokeTrustedDevice(deviceId, userId);

            if (revoked) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Устройство удалено из доверенных"
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Не удалось удалить устройство"));
            }

        } catch (Exception e) {
            log.error("Error revoking trusted device: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Ошибка при удалении устройства"));
        }
    }

    /**
     * POST /devices/api/revoke-current - API: Удалить текущее устройство из доверенных
     */
    @PostMapping("/api/revoke-current")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> revokeCurrentDevice(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Пользователь не найден"));
            }

            String deviceId = getDeviceIdFromCookie(request);
            if (deviceId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Устройство не найдено"));
            }

            boolean revoked = trustedDeviceService.revokeTrustedDeviceByDeviceId(deviceId, userId);

            if (revoked) {
                // Удаляем cookie
                removeDeviceIdCookie(response);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Текущее устройство удалено из доверенных"
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Устройство не является доверенным"));
            }

        } catch (Exception e) {
            log.error("Error revoking current device: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Ошибка при удалении устройства"));
        }
    }

    /**
     * GET /devices/api/stats - API: Статистика доверенных устройств
     */
    @GetMapping("/api/stats")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDeviceStats(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Пользователь не найден"));
            }

            List<TrustedDeviceService.TrustedDeviceDto> devices =
                    trustedDeviceService.getTrustedDevices(userId);

            // Группируем по типу устройства
            Map<String, Long> deviceTypes = devices.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            device -> extractDeviceType(device.getDeviceName()),
                            java.util.stream.Collectors.counting()
                    ));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "totalDevices", devices.size(),
                    "deviceTypes", deviceTypes,
                    "recentActivity", devices.stream()
                            .sorted((a, b) -> b.getLastUsedAt().compareTo(a.getLastUsedAt()))
                            .limit(3)
                            .toList()
            ));

        } catch (Exception e) {
            log.error("Error getting device stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Ошибка при получении статистики"));
        }
    }

    // Вспомогательные методы

    private String getDeviceIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("deviceId".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setDeviceIdCookie(HttpServletResponse response, String deviceId) {
        Cookie cookie = new Cookie("deviceId", deviceId);
        cookie.setMaxAge(60 * 60 * 24 * 365); // 1 год
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // для localhost, в продакшене должно быть true
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private void removeDeviceIdCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("deviceId", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private String extractDeviceName(String userAgent) {
        if (userAgent == null) return "Неизвестное устройство";

        if (userAgent.contains("Mobile")) {
            if (userAgent.contains("iPhone")) return "iPhone";
            if (userAgent.contains("iPad")) return "iPad";
            if (userAgent.contains("Android")) return "Android устройство";
            return "Мобильное устройство";
        }

        if (userAgent.contains("Windows")) return "Windows компьютер";
        if (userAgent.contains("Macintosh")) return "Mac компьютер";
        if (userAgent.contains("Linux")) return "Linux компьютер";

        return "Браузер";
    }

    private String extractDeviceType(String deviceName) {
        if (deviceName.contains("iPhone") || deviceName.contains("iPad")) return "Apple";
        if (deviceName.contains("Android")) return "Android";
        if (deviceName.contains("Windows")) return "Windows";
        if (deviceName.contains("Mac")) return "Mac";
        if (deviceName.contains("Linux")) return "Linux";
        return "Другое";
    }

    private Long getCurrentUserId(Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            return userOpt.map(User::getId).orElse(null);
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
            return null;
        }
    }

    // DTO для запросов
    @Getter
    @Setter
    public static class TrustDeviceRequest {
        private String deviceName;
    }
}