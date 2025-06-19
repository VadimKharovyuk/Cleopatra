package com.example.cleopatra.config.diagnostic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Контроллер для страницы нагрузочного тестирования
 * Отвечает за отображение HTML интерфейса управления тестами
 */
@Controller
@RequestMapping("/diagnostic")
@Slf4j
public class LoadTestPageController {

    /**
     * Главная страница нагрузочного тестирования
     */
    @GetMapping("/load-test")
    public String loadTestPage(Model model) {
        log.info("🔧 Открыта страница нагрузочного тестирования");

        // Добавляем базовые данные для страницы
        model.addAttribute("pageTitle", "Нагрузочное тестирование - Cleopatra");
        model.addAttribute("appName", "Cleopatra");
        model.addAttribute("version", "1.0.0");

        // Настройки по умолчанию для тестов
        model.addAttribute("defaultUrl", "https://cleopatra-brcc.onrender.com/profile/4");
        model.addAttribute("defaultUserId", 4);
        model.addAttribute("defaultRequests", 50);
        model.addAttribute("defaultConcurrency", 10);

        return "diagnostic/load-test";
    }

    /**
     * Страница результатов (если нужна отдельная)
     */
    @GetMapping("/load-test/results")
    public String loadTestResults(Model model) {
        log.info("📊 Открыта страница результатов тестирования");

        model.addAttribute("pageTitle", "Результаты тестирования - Cleopatra");

        return "diagnostic/load-test-results";
    }

    /**
     * Страница мониторинга системы
     */
    @GetMapping("/system-monitor")
    public String systemMonitorPage(Model model) {
        log.info("💻 Открыта страница мониторинга системы");

        model.addAttribute("pageTitle", "Мониторинг системы - Cleopatra");
        model.addAttribute("refreshInterval", 5000); // 5 секунд

        return "diagnostic/system-monitor";
    }
}