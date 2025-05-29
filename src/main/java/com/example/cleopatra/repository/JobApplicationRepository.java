package com.example.cleopatra.repository;

import com.example.cleopatra.enums.ApplicationStatus;
import com.example.cleopatra.model.JobApplication;
import com.example.cleopatra.enums.PerformerProfile;
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
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    // Поиск по email (для предотвращения дублирования)
    Optional<JobApplication> findByEmail(String email);

    // Поиск по статусу
    Page<JobApplication> findByStatus(ApplicationStatus status, Pageable pageable);
    List<JobApplication> findByStatus(ApplicationStatus status);

    // Поиск по профилю артиста
    Page<JobApplication> findByProfile(PerformerProfile profile, Pageable pageable);
    List<JobApplication> findByProfile(PerformerProfile profile);

    // Поиск по стране
    Page<JobApplication> findByCountry(String country, Pageable pageable);
    List<JobApplication> findByCountry(String country);

    // Комбинированный поиск
    Page<JobApplication> findByProfileAndStatus(PerformerProfile profile, ApplicationStatus status, Pageable pageable);
    Page<JobApplication> findByCountryAndStatus(String country, ApplicationStatus status, Pageable pageable);

    // Поиск по возрастному диапазону
    @Query("SELECT j FROM JobApplication j WHERE j.age BETWEEN :minAge AND :maxAge")
    Page<JobApplication> findByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge, Pageable pageable);

    // Поиск по опыту работы
    @Query("SELECT j FROM JobApplication j WHERE j.workExperience >= :minExperience")
    Page<JobApplication> findByMinimumExperience(@Param("minExperience") int minExperience, Pageable pageable);

    // Поиск по зарплатным ожиданиям
    @Query("SELECT j FROM JobApplication j WHERE j.minSalary <= :maxBudget OR j.minSalary IS NULL")
    Page<JobApplication> findByBudgetRange(@Param("maxBudget") java.math.BigDecimal maxBudget, Pageable pageable);

    // Полнотекстовый поиск по имени и биографии
    @Query("SELECT j FROM JobApplication j WHERE " +
            "LOWER(j.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(j.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(j.bio) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(j.additionalSkills) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<JobApplication> searchByText(@Param("query") String query, Pageable pageable);

    // Статистические запросы
    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.status = :status")
    long countByStatus(@Param("status") ApplicationStatus status);

    @Query("SELECT j.profile, COUNT(j) FROM JobApplication j GROUP BY j.profile")
    List<Object[]> countByProfile();

    @Query("SELECT j.country, COUNT(j) FROM JobApplication j GROUP BY j.country ORDER BY COUNT(j) DESC")
    List<Object[]> countByCountry();

    // Заявки за период
    @Query("SELECT j FROM JobApplication j WHERE j.createdAt BETWEEN :start AND :end ORDER BY j.createdAt DESC")
    List<JobApplication> findByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Недавние заявки
    @Query("SELECT j FROM JobApplication j ORDER BY j.createdAt DESC")
    List<JobApplication> findRecentApplications(Pageable pageable);

    // Заявки, требующие рассмотрения (старые pending)
    @Query("SELECT j FROM JobApplication j WHERE j.status = 'PENDING' AND j.createdAt < :before ORDER BY j.createdAt ASC")
    List<JobApplication> findOldPendingApplications(@Param("before") LocalDateTime before);

    // Заявки по готовности к путешествиям
    List<JobApplication> findByWillingToTravelTrue();

    // Заявки с видео
    @Query("SELECT j FROM JobApplication j WHERE j.videoUrl IS NOT NULL AND j.videoUrl != ''")
    List<JobApplication> findApplicationsWithVideo();

    // Заявки с соцсетями
    @Query("SELECT j FROM JobApplication j WHERE j.instagram IS NOT NULL OR j.facebook IS NOT NULL")
    List<JobApplication> findApplicationsWithSocialMedia();

    // Комплексный фильтр
    @Query("SELECT j FROM JobApplication j WHERE " +
            "(:profile IS NULL OR j.profile = :profile) AND " +
            "(:status IS NULL OR j.status = :status) AND " +
            "(:country IS NULL OR j.country = :country) AND " +
            "(:minAge IS NULL OR j.age >= :minAge) AND " +
            "(:maxAge IS NULL OR j.age <= :maxAge) AND " +
            "(:minExperience IS NULL OR j.workExperience >= :minExperience) AND " +
            "(:maxBudget IS NULL OR j.minSalary <= :maxBudget OR j.minSalary IS NULL) AND " +
            "(:willingToTravel IS NULL OR j.willingToTravel = :willingToTravel)")
    Page<JobApplication> findByFilters(
            @Param("profile") PerformerProfile profile,
            @Param("status") ApplicationStatus status,
            @Param("country") String country,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("minExperience") Integer minExperience,
            @Param("maxBudget") java.math.BigDecimal maxBudget,
            @Param("willingToTravel") Boolean willingToTravel,
            Pageable pageable
    );
}