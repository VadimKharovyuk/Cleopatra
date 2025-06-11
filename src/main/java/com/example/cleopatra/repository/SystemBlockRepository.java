package com.example.cleopatra.repository;

import com.example.cleopatra.model.SystemBlock;
import com.example.cleopatra.model.User;
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
public interface SystemBlockRepository extends JpaRepository<SystemBlock, Long> {

    // Найти активную блокировку пользователя
    Optional<SystemBlock> findByBlockedUserAndIsActiveTrue(User blockedUser);

    // Найти все блокировки пользователя (история)
    List<SystemBlock> findByBlockedUserOrderByBlockedAtDesc(User blockedUser);

    // Найти активные блокировки
    List<SystemBlock> findByIsActiveTrueOrderByBlockedAtDesc();

    // Найти блокировки по администратору
    List<SystemBlock> findByBlockedByAdminOrderByBlockedAtDesc(User admin);

    // Статистика блокировок за период
    @Query("SELECT COUNT(sb) FROM SystemBlock sb WHERE sb.blockedAt BETWEEN :startDate AND :endDate")
    Long countBlocksBetweenDates(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    // Найти блокировки с пагинацией
    Page<SystemBlock> findByIsActiveTrueOrderByBlockedAtDesc(Pageable pageable);

    // Проверить, есть ли активная блокировка у пользователя
    @Query("SELECT CASE WHEN COUNT(sb) > 0 THEN true ELSE false END FROM SystemBlock sb WHERE sb.blockedUser.id = :userId AND sb.isActive = true")
    boolean existsActiveBlockByUserId(@Param("userId") Long userId);
}