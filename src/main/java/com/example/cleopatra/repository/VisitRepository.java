package com.example.cleopatra.repository;

import com.example.cleopatra.model.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    boolean existsByVisitedUserIdAndVisitorIdAndVisitedAtAfter(
            Long visitedUserId, Long visitorId, LocalDateTime after);


    Page<Visit> findByVisitedUserIdOrderByVisitedAtDesc(Long userId, Pageable pageable);


    /**
     * Подсчет всех визитов к пользователю
     */
    Long countByVisitedUserId(Long visitedUserId);

    /**
     * Подсчет визитов к пользователю после определенной даты
     */
    Long countByVisitedUserIdAndVisitedAtAfter(Long visitedUserId, LocalDateTime after);

    /**
     * Подсчет уникальных посетителей пользователя
     */
    @Query("SELECT COUNT(DISTINCT v.visitor.id) FROM Visit v WHERE v.visitedUser.id = :userId")
    Long countUniqueVisitorsByUserId(@Param("userId") Long userId);

    /**
     * Получение уникальных визитов за период (для getUniqueVisitors)
     */
    /**
     * Получение уникальных визитов за период (для getUniqueVisitors)
     */
    @Query("SELECT v FROM Visit v " +
            "WHERE v.visitedUser.id = :userId " +
            "AND v.visitedAt BETWEEN :from AND :to " +
            "AND v.id IN (" +
            "  SELECT MAX(v2.id) FROM Visit v2 " +
            "  WHERE v2.visitedUser.id = :userId " +
            "  AND v2.visitedAt BETWEEN :from AND :to " +
            "  GROUP BY v2.visitor.id" +
            ") " +
            "ORDER BY v.visitedAt DESC")
    List<Visit> findUniqueVisitsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);




    // НОВЫЕ МЕТОДЫ ДЛЯ IP АНАЛИТИКИ

    @Query("SELECT COUNT(DISTINCT v.ipAddress) FROM Visit v WHERE v.visitedUser.id = :userId")
    Long countUniqueIpsByUserId(@Param("userId") Long userId);

    @Query("SELECT v.ipAddress, COUNT(v) FROM Visit v WHERE v.visitedUser.id = :userId " +
            "GROUP BY v.ipAddress ORDER BY COUNT(v) DESC")
    List<Object[]> findTopIpAddressesByUserId(@Param("userId") Long userId, Pageable pageable);

    Long countByIpAddressAndVisitedAtAfter(String ipAddress, LocalDateTime after);

    @Query("SELECT v FROM Visit v WHERE v.visitedUser.id = :userId AND v.ipAddress = :ipAddress " +
            "ORDER BY v.visitedAt DESC")
    List<Visit> findByUserIdAndIpAddress(@Param("userId") Long userId,
                                         @Param("ipAddress") String ipAddress,
                                         Pageable pageable);

}

