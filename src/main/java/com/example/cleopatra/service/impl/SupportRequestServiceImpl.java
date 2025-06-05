package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.Category;
import com.example.cleopatra.enums.Status;
import com.example.cleopatra.model.SupportRequest;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.SupportRequestRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.SupportRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportRequestServiceImpl implements SupportRequestService {
    private final SupportRequestRepository supportRequestRepository;
    private final UserRepository userRepository;

    @Override
    public SupportRequest createSupportRequest(String title, String description, Long userId, String categoryStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Category category = Category.valueOf(categoryStr.toUpperCase());

        SupportRequest supportRequest = SupportRequest.builder()
                .title(title)
                .description(description)
                .user(user)
                .category(category)
                .build();

        log.info("Создание новой заявки в поддержку от пользователя ID: {}", userId);

        SupportRequest saved = supportRequestRepository.save(supportRequest);

        // Здесь можно добавить отправку уведомления админам
        // notificationService.notifyAdmins(saved);

        return saved;
    }

    @Override
    public List<SupportRequest> getUserRequests(Long userId) {
        return supportRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<SupportRequest> getSupportRequestById(Long id) {
        return supportRequestRepository.findById(id);
    }

    @Override
    public List<SupportRequest> getActiveRequests() {
        return supportRequestRepository.findActiveRequests();
    }


    @Override
    public List<SupportRequest> getRequestsByStatus(Status status) {
        return supportRequestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public Optional<SupportRequest> updateStatus(Long id, Status newStatus, String adminResponse) {
        return supportRequestRepository.findById(id)
                .map(request -> {
                    Status oldStatus = request.getStatus();
                    request.setStatus(newStatus);

                    if (adminResponse != null && !adminResponse.trim().isEmpty()) {
                        request.setAdminResponse(adminResponse);
                    }

                    SupportRequest updated = supportRequestRepository.save(request);

                    log.info("Статус заявки {} изменен с {} на {}",
                            id, oldStatus, newStatus);

                    // Здесь можно добавить отправку уведомления пользователю
                    // notificationService.notifyUser(updated);

                    return updated;
                });
    }

    @Override
    public Optional<SupportRequest> closeRequest(Long id, String adminResponse) {
        return updateStatus(id, Status.CLOSED, adminResponse);
    }

    @Override
    public Optional<SupportRequest> takeInProgress(Long id) {
        return updateStatus(id, Status.IN_PROGRESS, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportStatistics getStatistics() {
        return SupportStatistics.builder()
                .totalRequests(supportRequestRepository.count())
                .openRequests(supportRequestRepository.countByStatus(Status.OPEN))
                .inProgressRequests(supportRequestRepository.countByStatus(Status.IN_PROGRESS))
                .resolvedRequests(supportRequestRepository.countByStatus(Status.RESOLVED))
                .closedRequests(supportRequestRepository.countByStatus(Status.CLOSED))
                .build();
    }
}
