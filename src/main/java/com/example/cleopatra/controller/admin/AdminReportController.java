// 1. Админский контроллер
package com.example.cleopatra.controller.admin;

import com.example.cleopatra.dto.ReportDTO.ReportResponseDTO;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.PostReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@Slf4j
public class AdminReportController {

    private final PostReportService reportService;
    private final UserRepository userRepository; // Добавляем UserRepository

    @GetMapping
    public String showReportsPage(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(defaultValue = "all") String filter,
                                  Authentication authentication,
                                  Model model) {
        try {
            // Получаем текущего админа по email
            String adminEmail = authentication.getName();
            User currentAdmin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            // Создаем пагинацию с сортировкой по дате создания
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<ReportResponseDTO> reports;

            // Фильтруем жалобы по статусу
            if ("pending".equals(filter)) {
                reports = reportService.getPendingReports(pageable);
            } else {
                reports = reportService.getAllReports(pageable);
            }

            // Добавляем данные в модель
            model.addAttribute("reports", reports);
            model.addAttribute("currentPage", page);
            model.addAttribute("currentFilter", filter);
            model.addAttribute("totalPages", reports.getTotalPages());
            model.addAttribute("totalElements", reports.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("currentAdmin", currentAdmin);

            // Статистика для дашборда
            model.addAttribute("pendingCount", getPendingCount());
            model.addAttribute("totalCount", reports.getTotalElements());

            log.info("Админ {} загрузил страницу жалоб: страница {}, фильтр {}",
                    adminEmail, page, filter);

            return "admin/reports/list";

        } catch (Exception e) {
            log.error("Ошибка при загрузке жалоб для админа {}",
                    authentication.getName(), e);
            model.addAttribute("error", "Ошибка при загрузке данных: " + e.getMessage());
            return "admin/reports/list";
        }
    }

    @PostMapping("/{reportId}/resolve")
    @ResponseBody
    public ResponseEntity<?> resolveReport(@PathVariable Long reportId,
                                           @RequestParam String adminComment,
                                           @RequestParam String actionTaken,
                                           Authentication authentication) {
        try {
            // Получаем текущего админа по email
            String adminEmail = authentication.getName();
            User currentAdmin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            reportService.resolveReport(reportId, currentAdmin.getId(), adminComment, actionTaken);

            log.info("Жалоба {} успешно рассмотрена админом {} ({})",
                    reportId, currentAdmin.getFirstName() + " " + currentAdmin.getLastName(), adminEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Жалоба успешно рассмотрена"
            ));

        } catch (RuntimeException e) {
            log.warn("Ошибка при рассмотрении жалобы {}: {}", reportId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Неожиданная ошибка при рассмотрении жалобы {} админом {}",
                    reportId, authentication.getName(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Произошла ошибка сервера"));
        }
    }

    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<?> getReportStats(Authentication authentication) {
        try {
            // Получаем админа для логирования
            String adminEmail = authentication.getName();

            Map<String, Object> stats = Map.of(
                    "pending", getPendingCount(),
                    "total", getTotalCount(),
                    "today", getTodayCount()
            );

            log.debug("Админ {} запросил статистику жалоб", adminEmail);
            return ResponseEntity.ok(Map.of("success", true, "stats", stats));

        } catch (Exception e) {
            log.error("Ошибка при получении статистики жалоб для админа {}",
                    authentication.getName(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Ошибка загрузки статистики"));
        }
    }

    // Остальные методы без изменений...
    private long getPendingCount() {
        try {
            Pageable pageable = PageRequest.of(0, 1);
            return reportService.getPendingReports(pageable).getTotalElements();
        } catch (Exception e) {
            log.warn("Ошибка получения количества pending жалоб: {}", e.getMessage());
            return 0;
        }
    }

    private long getTotalCount() {
        try {
            Pageable pageable = PageRequest.of(0, 1);
            return reportService.getAllReports(pageable).getTotalElements();
        } catch (Exception e) {
            log.warn("Ошибка получения общего количества жалоб: {}", e.getMessage());
            return 0;
        }
    }

    private long getTodayCount() {
        // TODO: Добавить метод в сервис для подсчета жалоб за сегодня
        return 0;
    }
}