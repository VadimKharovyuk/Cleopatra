package com.example.cleopatra.config.diagnostic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Контроллер для диагностики системы
 * Использует OptimizedSystemMonitor для получения метрик
 */
@RestController
@RequestMapping("/diagnostic")
public class DiagnosticController {

    @Autowired
    private OptimizedSystemMonitor systemMonitor;

    /**
     * Информация о памяти JVM
     */
    @GetMapping("/memory")
    public Map<String, Object> getMemoryInfo() {
        return systemMonitor.getMemoryInfo();
    }

    /**
     * Полная информация о системе (память + DB pool + система)
     * Использует другой путь, чтобы избежать конфликта с HealthController
     */
    @GetMapping("/full-system")
    public Map<String, Object> getFullSystemInfo() {
        return systemMonitor.getSystemInfo();
    }
}