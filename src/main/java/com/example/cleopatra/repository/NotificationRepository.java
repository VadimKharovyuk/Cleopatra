package com.example.cleopatra.repository;


import com.example.cleopatra.enums.NotificationType;
import com.example.cleopatra.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipientId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :userId AND n.isRead = false")
    long countUnreadByRecipientId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE " +
            "n.recipient.id = :recipientId AND " +
            "n.actor.id = :actorId AND " +
            "n.type = :type AND " +
            "n.createdAt > :threshold")
    boolean existsByRecipientIdAndActorIdAndTypeAndCreatedAtAfter(
            @Param("recipientId") Long recipientId,
            @Param("actorId") Long actorId,
            @Param("type") NotificationType type,
            @Param("threshold") LocalDateTime threshold
    );

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
            "WHERE n.recipient.id = :userId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("userId") Long userId);
}
