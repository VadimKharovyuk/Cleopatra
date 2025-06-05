package com.example.cleopatra.repository;

import com.example.cleopatra.enums.Status;
import com.example.cleopatra.model.SupportRequest;
import com.example.cleopatra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    // Найти по пользователю
    List<SupportRequest> findByUserOrderByCreatedAtDesc(User user);

    // Найти по ID пользователя
    List<SupportRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Найти по статусу
    List<SupportRequest> findByStatusOrderByCreatedAtDesc(Status status);

    // Подсчет по статусам
    long countByStatus(Status status);

    @Query("SELECT sr FROM SupportRequest sr WHERE sr.status IN ('OPEN', 'IN_PROGRESS') ORDER BY sr.createdAt DESC")
    List<SupportRequest> findActiveRequests();
}
