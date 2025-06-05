package com.example.cleopatra.service;

import com.example.cleopatra.enums.Status;
import com.example.cleopatra.model.SupportRequest;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

public interface SupportRequestService {

    SupportRequest createSupportRequest(String title, String description, Long userId, String categoryStr);

    List<SupportRequest> getUserRequests(Long userId);

    Optional<SupportRequest> getSupportRequestById(Long id);

    List<SupportRequest> getActiveRequests();

    List<SupportRequest> getRequestsByStatus(Status status);

    Optional<SupportRequest> updateStatus(Long id, Status newStatus, String adminResponse);

    Optional<SupportRequest> closeRequest(Long id, String adminResponse);

    Optional<SupportRequest> takeInProgress(Long id);

    // Добавляем метод статистики
    SupportStatistics getStatistics();



    /**
     * Класс для статистики заявок
     */
    @Data
    @Builder
    class SupportStatistics {
        private long totalRequests;
        private long openRequests;
        private long inProgressRequests;
        private long resolvedRequests;
        private long closedRequests;
    }
}