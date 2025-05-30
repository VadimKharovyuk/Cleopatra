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


@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    /**
     * Комплексный поиск с фильтрами
     */
    @Query("SELECT ja FROM JobApplication ja WHERE " +
            "(:status IS NULL OR ja.status = :status) AND " +
            "(:profile IS NULL OR ja.profile = :profile) AND " +
            "(:country IS NULL OR LOWER(ja.country) = LOWER(:country)) AND " +
            "(:searchQuery IS NULL OR " +
            " LOWER(ja.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            " LOWER(ja.email) LIKE LOWER(CONCAT('%', :searchQuery, '%')))")
    Page<JobApplication> findWithFilters(
            @Param("status") ApplicationStatus status,
            @Param("profile") PerformerProfile profile,
            @Param("country") String country,
            @Param("searchQuery") String searchQuery,
            Pageable pageable);
}