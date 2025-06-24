package com.example.cleopatra.config.diagnostic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/diagnostic")
@Slf4j
public class LoadTestPageController {


    @GetMapping("/load-test")
    public String loadTestPage(Model model) {

        // Добавляем базовые данные для страницы
        model.addAttribute("pageTitle", "Нагрузочное тестирование - Cleopatra");
        model.addAttribute("appName", "Cleopatra");
        model.addAttribute("version", "1.0.0");

        model.addAttribute("defaultUrl", "https://cleopatra-brcc.onrender.com/profile/4");
        model.addAttribute("defaultUserId", 4);
        model.addAttribute("defaultRequests", 50);
        model.addAttribute("defaultConcurrency", 10);

        return "diagnostic/load-test";
    }

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
        model.addAttribute("pageTitle", "Мониторинг системы - Cleopatra");
        model.addAttribute("refreshInterval", 5000); // 5 секунд

        return "diagnostic/system-monitor";
    }
}