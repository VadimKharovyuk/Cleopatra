package com.example.cleopatra.repository;

import com.example.cleopatra.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.cleopatra.model.Advertisement;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.enums.AdCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    // Для админ панели - все рекламы
    Page<Advertisement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Рекламы по статусу
    Page<Advertisement> findByStatusOrderByCreatedAtDesc(AdStatus status, Pageable pageable);

    // Рекламы требующие модерации
    Page<Advertisement> findByStatusInOrderByCreatedAtDesc(java.util.List<AdStatus> statuses, Pageable pageable);

    // Рекламы конкретного пользователя
    Page<Advertisement> findByCreatedByIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Рекламы с жалобами
    @Query("SELECT DISTINCT a FROM Advertisement a JOIN a.reports r ORDER BY a.createdAt DESC")
    Page<Advertisement> findAdvertisementsWithReports(Pageable pageable);

    // Активные рекламы для показа (с фильтрацией)
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'ACTIVE' " +
            "AND a.remainingBudget >= a.costPerView " +
            "AND (a.startDate IS NULL OR a.startDate <= CURRENT_DATE) " +
            "AND (a.endDate IS NULL OR a.endDate >= CURRENT_DATE) " +
            "AND (a.startTime IS NULL OR a.startTime <= CURRENT_TIME) " +
            "AND (a.endTime IS NULL OR a.endTime >= CURRENT_TIME) " +
            "AND (:targetGender IS NULL OR a.targetGender IS NULL OR a.targetGender = :targetGender) " +
            "AND (:userAge IS NULL OR a.minAge IS NULL OR a.minAge <= :userAge) " +
            "AND (:userAge IS NULL OR a.maxAge IS NULL OR a.maxAge >= :userAge) " +
            "AND (:userCity IS NULL OR a.targetCity IS NULL OR a.targetCity = :userCity) " +
            "ORDER BY RANDOM()")
    Page<Advertisement> findActiveAdsForUser(
            @Param("targetGender") String targetGender,
            @Param("userAge") Integer userAge,
            @Param("userCity") String userCity,
            Pageable pageable
    );

    // Поиск по названию для админ панели
    @Query("SELECT a FROM Advertisement a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "ORDER BY a.createdAt DESC")
    Page<Advertisement> searchAdvertisements(@Param("search") String search, Pageable pageable);

    // Статистика для дашборда
    @Query("SELECT COUNT(a) FROM Advertisement a WHERE a.status = :status")
    Long countByStatus(@Param("status") AdStatus status);

    @Query("SELECT a.category, COUNT(a) FROM Advertisement a GROUP BY a.category")
    java.util.List<Object[]> getAdCountByCategory();
}