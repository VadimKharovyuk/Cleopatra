package com.example.cleopatra.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Slf4j
public class HealthController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("🏥 Health ping called");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Application is running");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> systemInfo() {
        log.info("🔍 System info called");

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("java.version", System.getProperty("java.version"));
            response.put("spring.profiles.active", System.getProperty("spring.profiles.active"));
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error in system info: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }
}
