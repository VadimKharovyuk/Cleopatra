package com.example.cleopatra.repository;
import com.example.cleopatra.model.QrAuthSession;
import com.example.cleopatra.enums.QrAuthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QrAuthSessionRepository extends JpaRepository<QrAuthSession, Long> {

    // Найти сессию по токену
    Optional<QrAuthSession> findByToken(String token);

    // Найти активную сессию по токену (не истекшую)
    @Query("SELECT q FROM QrAuthSession q WHERE q.token = :token " +
            "AND q.expiresAt > :now AND q.status != 'EXPIRED'")
    Optional<QrAuthSession> findActiveByToken(
            @Param("token") String token,
            @Param("now") LocalDateTime now
    );

    // Удалить все просроченные сессии
    @Modifying
    @Transactional
    @Query("DELETE FROM QrAuthSession q WHERE q.expiresAt < :now " +
            "OR q.status = 'EXPIRED'")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);

    // Добавьте в QrAuthSessionRepository
    @Query("SELECT COUNT(q) FROM QrAuthSession q WHERE q.status = :status")
    int countByStatus(@Param("status") QrAuthStatus status);
    // Пометить просроченные сессии как EXPIRED
    @Modifying
    @Transactional
    @Query("UPDATE QrAuthSession q SET q.status = 'EXPIRED' " +
            "WHERE q.expiresAt < :now AND q.status = 'PENDING'")
    int markExpiredSessions(@Param("now") LocalDateTime now);

    // Подтвердить сессию
    @Modifying
    @Transactional
    @Query("UPDATE QrAuthSession q SET q.status = 'CONFIRMED', q.userId = :userId " +
            "WHERE q.token = :token AND q.status = 'PENDING'")
    int confirmSession(@Param("token") String token, @Param("userId") Long userId);
    // Проверить статус сессии
    @Query("SELECT q.status FROM QrAuthSession q WHERE q.token = :token")
    Optional<QrAuthStatus> findStatusByToken(@Param("token") String token);



}