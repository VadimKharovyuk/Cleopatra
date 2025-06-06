package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.game.LazyOpenTDBQuestionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LazyOpenTDBQuestionProvider questionProvider;



    @GetMapping("/cache-stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("cacheStats", questionProvider.getCacheStats());
        response.put("apiAvailable", questionProvider.isAvailable());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

//    /**
//     * Принудительно обновить кэш
//     */
//    @GetMapping("/refresh-cache")
//    public Map<String, Object> refreshCache() {
//        Map<String, Object> response = new HashMap<>();
//        try {
//            // Запускаем в отдельном потоке чтобы не блокировать HTTP запрос
//            new Thread(() -> {
//                try {
//                    questionProvider.replenishCache();
//                } catch (Exception e) {
//                    log.error("Error during manual cache refresh", e);
//                }
//            }).start();
//
//            response.put("success", true);
//            response.put("message", "Cache refresh initiated in background");
//        } catch (Exception e) {
//            response.put("success", false);
//            response.put("error", e.getMessage());
//        }
//        return response;
//    }
}