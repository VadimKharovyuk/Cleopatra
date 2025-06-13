package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.ReportDTO.CreateReportDTO;
import com.example.cleopatra.service.PostReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class PostReportRestController {

    private final PostReportService reportService;

    @PostMapping
    public ResponseEntity<?> createReport(@Valid @RequestBody CreateReportDTO dto,
                                          @ModelAttribute("currentUserId") Long currentUserId,
                                          HttpServletRequest request) {
        try {
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Необходима авторизация"));
            }

            String reporterIp = getClientIpAddress(request);
            reportService.createReport(dto, currentUserId, reporterIp);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Жалоба успешно отправлена. Спасибо за помощь в улучшении платформы!"
            ));

        } catch (RuntimeException e) {
            log.warn("Ошибка при создании жалобы: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании жалобы", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Произошла ошибка. Попробуйте позже."));
        }
    }

    // Получение доступных причин жалоб для фронтенда
    @GetMapping("/reasons")
    public ResponseEntity<?> getReportReasons() {
        try {
            var reasons = com.example.cleopatra.enums.ReportReason.values();
            var reasonsList = java.util.Arrays.stream(reasons)
                    .map(reason -> Map.of(
                            "value", reason.name(),
                            "description", reason.getDescription()
                    ))
                    .toList();

            return ResponseEntity.ok(Map.of("success", true, "reasons", reasonsList));
        } catch (Exception e) {
            log.error("Ошибка при получении причин жалоб", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Ошибка загрузки данных"));
        }
    }

    // Метод для получения IP адреса клиента
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null || xForwardedForHeader.isEmpty()) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0].trim();
        }
    }
}

