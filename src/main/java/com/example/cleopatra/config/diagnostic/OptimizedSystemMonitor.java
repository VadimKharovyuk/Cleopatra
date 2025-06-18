package com.example.cleopatra.config.diagnostic;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Оптимизированный мониторинг системы - ТОЛЬКО шедулеры
 * Отделен от REST контроллеров во избежание конфликтов
 */
@Component
@Slf4j
public class OptimizedSystemMonitor {

    @Autowired
    private DataSource dataSource;

    // Кэшированные объекты для избежания частого создания
    private static final Runtime runtime = Runtime.getRuntime();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    // Счетчики для статистики
    private final AtomicLong monitoringCycles = new AtomicLong(0);
    private long startTime = System.currentTimeMillis();

    @PostConstruct
    public void logStartupInfo() {
        log.info("=== Cleopatra System Monitor Started ===");
        log.info("Max Memory: {} MB", runtime.maxMemory() / 1024 / 1024);
        log.info("Initial Memory: {} MB", runtime.totalMemory() / 1024 / 1024);
        log.info("Available processors: {}", runtime.availableProcessors());

        // Логируем GC только один раз при старте
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        gcBeans.forEach(gc -> log.info("GC: {}", gc.getName()));

        startTime = System.currentTimeMillis();
        log.info("=== Monitor initialization completed ===");
    }

    /**
     * Основной мониторинг - каждые 2 минуты
     * Объединяет память + DB pool + производительность
     */
    @Scheduled(fixedRate = 120000) // 2 минуты
    public void performComprehensiveMonitoring() {
        try {
            long cycle = monitoringCycles.incrementAndGet();

            // Память
            MemoryStats memory = getMemoryStats();

            // Database Pool
            PoolStats pool = getDatabasePoolStats();

            // Основной лог - компактный формат
            log.info("Monitor #{} - Memory: {}/{} MB ({}%), Pool: {}/{}, Uptime: {} min",
                    cycle,
                    memory.usedMB, memory.totalMB, memory.usagePercent,
                    pool.active, pool.total,
                    (System.currentTimeMillis() - startTime) / 60000);

            // Детальное логирование только при проблемах
            if (memory.usagePercent > 80 || pool.active >= pool.total) {
                log.warn("HIGH USAGE DETECTED - Heap: {} MB, Non-Heap: {} MB, Pool waiting: {}",
                        memory.heapUsedMB, memory.nonHeapUsedMB, pool.waiting);

                // Принудительная сборка мусора при высоком использовании
                if (memory.usagePercent > 85) {
                    System.gc();
                    log.info("Forced GC triggered due to high memory usage");
                }
            }

        } catch (Exception e) {
            log.error("Monitoring error: {}", e.getMessage());
        }
    }

    /**
     * Быстрая проверка критичных метрик - каждые 5 минут
     * Только важная информация без лишних объектов
     */
    @Scheduled(fixedRate = 300000) // 5 минут
    public void performQuickHealthCheck() {
        try {
            long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            double usagePercent = (double) usedMB / (runtime.totalMemory() / 1024 / 1024) * 100;

            // Логируем только при проблемах
            if (usagePercent > 75) {
                log.warn("Memory check - {} MB used ({}% of allocated)", usedMB, String.format("%.1f", usagePercent));
            }

        } catch (Exception e) {
            log.error("Quick health check error: {}", e.getMessage());
        }
    }

    // Публичные методы для использования в REST контроллерах
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> result = new HashMap<>(8);

        try {
            long maxMB = runtime.maxMemory() / 1024 / 1024;
            long totalMB = runtime.totalMemory() / 1024 / 1024;
            long freeMB = runtime.freeMemory() / 1024 / 1024;
            long usedMB = totalMB - freeMB;

            result.put("maxMemory", maxMB + " MB");
            result.put("totalMemory", totalMB + " MB");
            result.put("usedMemory", usedMB + " MB");
            result.put("freeMemory", freeMB + " MB");
            result.put("usagePercent", String.format("%.1f%%", (double) usedMB / totalMB * 100));

            MemoryUsage heap = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
            result.put("heapUsed", heap.getUsed() / 1024 / 1024 + " MB");
            result.put("nonHeapUsed", nonHeap.getUsed() / 1024 / 1024 + " MB");

            result.put("uptimeMinutes", (System.currentTimeMillis() - startTime) / 60000);

        } catch (Exception e) {
            result.put("error", "Cannot retrieve memory info: " + e.getMessage());
        }

        return result;
    }

    public Map<String, Object> getSystemInfo() {
        Map<String, Object> result = new HashMap<>();

        // Memory
        result.put("memory", getMemoryInfo());

        // Database Pool
        if (dataSource instanceof HikariDataSource) {
            HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
            Map<String, Object> poolInfo = new HashMap<>();
            poolInfo.put("active", pool.getActiveConnections());
            poolInfo.put("idle", pool.getIdleConnections());
            poolInfo.put("total", pool.getTotalConnections());
            poolInfo.put("waiting", pool.getThreadsAwaitingConnection());
            result.put("databasePool", poolInfo);
        }

        // System
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("processors", runtime.availableProcessors());
        systemInfo.put("monitoringCycles", monitoringCycles.get());
        systemInfo.put("uptimeMinutes", (System.currentTimeMillis() - startTime) / 60000);
        result.put("system", systemInfo);

        return result;
    }

    // Вспомогательные методы
    private MemoryStats getMemoryStats() {
        long totalMB = runtime.totalMemory() / 1024 / 1024;
        long freeMB = runtime.freeMemory() / 1024 / 1024;
        long usedMB = totalMB - freeMB;

        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        return new MemoryStats(
                usedMB, totalMB,
                heap.getUsed() / 1024 / 1024,
                nonHeap.getUsed() / 1024 / 1024,
                (int) ((double) usedMB / totalMB * 100)
        );
    }

    private PoolStats getDatabasePoolStats() {
        if (dataSource instanceof HikariDataSource) {
            HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
            return new PoolStats(
                    pool.getActiveConnections(),
                    pool.getTotalConnections(),
                    pool.getThreadsAwaitingConnection()
            );
        }
        return new PoolStats(0, 0, 0);
    }

    // Внутренние классы для структурированных данных
    private static class MemoryStats {
        final long usedMB, totalMB, heapUsedMB, nonHeapUsedMB;
        final int usagePercent;

        MemoryStats(long usedMB, long totalMB, long heapUsedMB, long nonHeapUsedMB, int usagePercent) {
            this.usedMB = usedMB;
            this.totalMB = totalMB;
            this.heapUsedMB = heapUsedMB;
            this.nonHeapUsedMB = nonHeapUsedMB;
            this.usagePercent = usagePercent;
        }
    }

    private static class PoolStats {
        final int active, total, waiting;

        PoolStats(int active, int total, int waiting) {
            this.active = active;
            this.total = total;
            this.waiting = waiting;
        }
    }
}