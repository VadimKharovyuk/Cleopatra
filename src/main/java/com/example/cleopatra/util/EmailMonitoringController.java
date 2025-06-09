package com.example.cleopatra.util;

import com.example.cleopatra.service.impl.EmailQueueManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
public class EmailMonitoringController {

    private final EmailQueueManager emailQueueManager;

    @GetMapping("/queue-status")
    public Map<String, Object> getQueueStatus() {
        return Map.of(
                "queueSize", emailQueueManager.getQueueSize(),
                "processingCount", emailQueueManager.getProcessingCount(),
                "status", "running"
        );
    }

    // Вариант 2: JSON в body
    @PostMapping("/test-email-json")
    public ResponseEntity<Map<String, String>> testEmailJson(@RequestBody TestEmailRequest request) {
        emailQueueManager.sendPasswordResetEmail(request.getEmail(), "TEST123");
        return ResponseEntity.ok(Map.of(
                "message", "Email queued successfully",
                "email", request.getEmail(),
                "queueSize", String.valueOf(emailQueueManager.getQueueSize())
        ));
    }
    // DTO для JSON requests
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestEmailRequest {
        private String email;
        private String password; // опционально
    }
}