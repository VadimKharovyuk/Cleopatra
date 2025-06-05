package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.Support.CreateSupportRequestDto;
import com.example.cleopatra.enums.Status;
import com.example.cleopatra.model.SupportRequest;
import com.example.cleopatra.service.SupportRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupportRequestController {


    private final SupportRequestService supportRequestService;

    // Создать новую заявку
    @PostMapping
    public ResponseEntity<SupportRequest> createSupportRequest(
            @Valid @RequestBody CreateSupportRequestDto requestDto) {

        SupportRequest saved = supportRequestService.createSupportRequest(
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getUserId(),
                requestDto.getCategory()
        );
        return ResponseEntity.ok(saved);
    }

    // Получить все заявки пользователя
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SupportRequest>> getUserRequests(@PathVariable Long userId) {
        List<SupportRequest> requests = supportRequestService.getUserRequests(userId);
        return ResponseEntity.ok(requests);
    }

    // Получить заявку по ID
    @GetMapping("/{id}")
    public ResponseEntity<SupportRequest> getSupportRequest(@PathVariable Long id) {
        return supportRequestService.getSupportRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Для админов - получить все активные заявки
    @GetMapping("/admin/active")
    public ResponseEntity<List<SupportRequest>> getActiveRequests() {
        List<SupportRequest> requests = supportRequestService.getActiveRequests();
        return ResponseEntity.ok(requests);
    }

    // Для админов - обновить статус заявки
    @PutMapping("/admin/{id}/status")
    public ResponseEntity<SupportRequest> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String adminResponse) {

        return supportRequestService.updateStatus(id,
                        Status.valueOf(status), adminResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Для админов - получить статистику
    @GetMapping("/admin/statistics")
    public ResponseEntity<SupportRequestService.SupportStatistics> getStatistics() {
        SupportRequestService.SupportStatistics stats = supportRequestService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}