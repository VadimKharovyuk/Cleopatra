package com.example.cleopatra.repository;


import com.example.cleopatra.enums.ReportReason;
import com.example.cleopatra.enums.ReportStatus;
import com.example.cleopatra.model.PostReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    // Найти все жалобы по статусу
    Page<PostReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    // Найти все жалобы на конкретный пост
    List<PostReport> findByPostIdOrderByCreatedAtDesc(Long postId);

    // Проверить, жаловался ли уже этот пользователь на этот пост
    boolean existsByPostIdAndReporterId(Long postId, Long reporterId);

    // Найти жалобы по приоритету
    Page<PostReport> findByPriorityGreaterThanEqualOrderByCreatedAtDesc(Integer priority, Pageable pageable);

    // Найти жалобы по причине
    Page<PostReport> findByReasonOrderByCreatedAtDesc(ReportReason reason, Pageable pageable);

    // Найти жалобы за период
    @Query("SELECT r FROM PostReport r WHERE r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<PostReport> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Статистика по жалобам
    @Query("SELECT COUNT(r) FROM PostReport r WHERE r.status = :status")
    Long countByStatus(@Param("status") ReportStatus status);

    @Query("SELECT COUNT(r) FROM PostReport r WHERE r.createdAt >= :since")
    Long countReportsToday(@Param("since") LocalDateTime since);

    // Найти наиболее проблемные посты
    @Query("SELECT r.post.id, COUNT(r) as reportCount FROM PostReport r " +
            "GROUP BY r.post.id ORDER BY reportCount DESC")
    List<Object[]> findMostReportedPosts(Pageable pageable);

    // Найти наиболее активных жалующихся
    @Query("SELECT r.reporter.id, COUNT(r) as reportCount FROM PostReport r " +
            "GROUP BY r.reporter.id ORDER BY reportCount DESC")
    List<Object[]> findMostActiveReporters(Pageable pageable);
}
